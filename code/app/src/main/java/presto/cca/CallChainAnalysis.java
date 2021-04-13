package presto.cca;

import java.util.*;
import java.io.*;

import presto.Util;
import presto.Config;

class CallChainAnalysis {
	// directory with the raw data
	public static String dir;
	// number of users in the raw data (accounting for replication)
	public static int num_users_opt_out;
	// size of the set V of static methods
	public static int size_v;
	// list of all traces from the raw data (before replication)
	// public static List<List<String>> traces = new ArrayList<List<String>>();
	// list of all trace file names
	public static ArrayList<String> list;
	// list of all treees from the raw data (before replication)
	// public static LinkedList<Tree> trees_opt_out = new LinkedList<Tree>();
	// number of unique nodes in all trees
	public static int num_nodes_opt_in;
	public static int num_nodes_opt_out;
	// for each method id, the method ids of its static callees
	public static Map<Integer,Set<Integer>> call_pairs;
	// global sketch, accumulating the effects of all local sketches
	public static Sketch global_sketch;
	// for performance, cache some of the estimated frequencies
	static Map<String,Integer> cached_estimates = new HashMap<String,Integer>();
	// heavy hitters from the ground truth   
	public static Set<String> real_heavy_hitters = new HashSet<String>();
	// heavy hitters from the estimates
	public static Set<String> estimated_heavy_hitters = new HashSet<String>();
	// app name
	public static String app_name;

	public static void main(String[] args) {
		parseArgs(args);

		// get all traces and static call pairs
		System.out.print("*** Reading list and pairs from '" + dir + "' ... ");
		call_pairs = Util.readCallPairs(dir + "/callpairs");
		list = Util.readList(dir + "/list");
		size_v = Util.readSizeV(dir + "/v");
		int num_users = Config.replication * list.size();
		System.out.println(" done. [" + list.size() + " traces to read; will use " + Config.replication + "x replication]");

		// split users into opt-in and opt-out
		// list of index to traces, used for drawing random users
		LinkedList<Integer> all_trace_idx = new LinkedList<>();
		for (int j = 0; j < Config.replication; j++) {
			for (int i = 0; i < list.size(); i++) { all_trace_idx.add(i); }
		}
		Collections.shuffle(all_trace_idx);
		// counter representing how many times each trace appears in opt-in/out set
		int[] opt_in_trace_ctr = new int[list.size()];
		int[] opt_out_trace_ctr = new int[list.size()];
		for (int i = 0; i < opt_out_trace_ctr.length; i++) {
			opt_out_trace_ctr[i] = Config.replication;
			opt_in_trace_ctr[i] = 0;
		}
		double opt_in_rate = 0.1;
		num_users_opt_out = (int)(num_users * (1 - opt_in_rate));
		int num_users_opt_in = num_users - num_users_opt_out;
		Random random = new Random();
		for (int i = 0; i < num_users_opt_in; i++) {
			int rand = random.nextInt(all_trace_idx.size());
			int idx = all_trace_idx.remove(rand);
			opt_in_trace_ctr[idx]++;
			opt_out_trace_ctr[idx]--;
		}

		// read traces and build individual trees
		System.out.print("*** Reading traces and building trees for opt-in, num_users:" + num_users_opt_in + " ... ");
		int global_max_depth_opt_in = 0;
		for (int i = 0; i < list.size(); i++) {
			if (opt_in_trace_ctr[i] == 0) continue;
			String fileName = list.get(i);
			List<String> trace = Util.readOneTrace(dir + "/" + fileName);
			for (int j = 0; j < opt_in_trace_ctr[i]; j++) {
				Tree t = new Tree(); t.addTrace(trace);
				if (global_max_depth_opt_in < t.max_depth) global_max_depth_opt_in = t.max_depth;
			}
		}
		num_nodes_opt_in = Tree.union_ground_truth.size();
		System.out.println("done. [total nodes: " + num_nodes_opt_in +
						   ", maxdepth: " + global_max_depth_opt_in + ", v: " + size_v + "]");

		// building sketches while building trees for opt-out, to save memory
		// set the depth limit as max_depth_opt_in
		// Config.depth_limit = global_max_depth_optin;
		// build tree sketches and from them the global sketch
		Sketch.init(num_nodes_opt_in);
		global_sketch = new Sketch();
		System.out.println("*** Building global sketch [t=" + Sketch.t +
						 ",m=" + Sketch.m + ",log(m)="+Sketch.log_m + ",epsilon=" + Sketch.epsilon + "] ...    ");
		System.out.print("*** Reading traces and building trees for opt-out, num_users:" + num_users_opt_out + " ...    ");
		System.out.flush();
		Tree.union_ground_truth = new HashMap<>();
		int global_max_depth_opt_out = 0;
		int num_users_done = 0;
		for (int i = 0; i < list.size(); i++) {
			if (opt_out_trace_ctr[i] == 0) continue;
			String fileName = list.get(i);
			List<String> trace = Util.readOneTrace(dir + "/" + fileName);
			for (int j = 0; j < opt_out_trace_ctr[i]; j++) {
				Tree t = new Tree(); t.addTrace(trace);
				if (global_max_depth_opt_out < t.max_depth) global_max_depth_opt_out = t.max_depth;
				global_sketch.addContributionOf(new Sketch(t));
				if (Config.print_progress) {
					num_users_done++;
					int done = (100 * (num_users_done-1)) / num_users_opt_out;
					if (done >= 10) {
						System.out.print("\b\b\b" + done + "%"); System.out.flush();
					} else {
						System.out.print("\b\b\b " + done + "%"); System.out.flush();
					}
				}
			}
		}
		num_nodes_opt_out = Tree.union_ground_truth.size();
		System.out.println("\b\b\b 100% done. [total nodes: " + num_nodes_opt_out +
							", maxdepth: " + global_max_depth_opt_out + ", v: " + size_v + "]");
		/* System.out.println(app_name +
							", ratio: " + ((double)num_nodes_opt_in)/num_nodes_opt_out);*/
		// no need for list anymore; release the memory
		list.clear();

		String RE = computeRE(); // for ground-truth nodes
		/* System.out.println(app_name +
						   ", RE:" + RE);
		System.exit(0); */
		// real heavy hitters
		int limit = (Config.hh_cutoff * num_users_opt_out)/100 ;
		for (String long_id : Tree.union_ground_truth.keySet()) {
			int ground_freq = Tree.union_ground_truth.get(long_id);
			if (ground_freq >= limit) real_heavy_hitters.add(long_id);
		}
		// estimated heavy hitters
		System.out.print("*** Computing heavy hitters, depth_limit=" + Config.depth_limit + 
			(Config.strictMode ? "strict" : "relaxed") + " ...    "); System.out.flush();
		computeHeavyHitters(limit);
		System.out.println("\b\b\b 100% done."); System.out.flush();
		// precision and recall
		Set<String> inter = new HashSet<String>(real_heavy_hitters);
		inter.retainAll(estimated_heavy_hitters);
		String recall = Util.rel(inter.size(),real_heavy_hitters.size());
		String precision = Util.rel(inter.size(),estimated_heavy_hitters.size());

		if (Config.printFalse) {
			List<String> missed = new ArrayList<String>();
			for (String s : real_heavy_hitters) 
				if (!inter.contains(s)) missed.add(s);
			Collections.sort(missed);
			for (String s : missed) 
					System.out.println("missed: " + s);

			List<String> falseP = new ArrayList<String>();
			for (String s: estimated_heavy_hitters) 
				if (!inter.contains(s)) falseP.add(s);
			Collections.sort(falseP);
			for (String s: falseP) {
					System.out.println("falseP: " + s + 
							"  ground_freq: " + Tree.union_ground_truth.getOrDefault(s, 0) + 
							" estimated_req: " + freqEstimate(s));
			}
		}

		String REforEHH = computeREforEHH(); // for estimated heavy hitters
		// print final metrics
		System.out.println("*** Call-chain Analysis Result for '" + app_name + "' ***");
		System.out.println("Error All: " + RE);
		System.out.println("Error Hot: " + REforEHH);
		System.out.println("Recall: " + recall);
		System.out.println("Precision: " + precision);
		// dump the frequencies for debugging
		/* System.out.println("Printing frequencies of all chains...");
		for (String long_id: Tree.union_ground_truth.keySet()) {
			int ground_freq = Tree.union_ground_truth.get(long_id);
			int est_freq = freqEstimate(long_id);
			System.out.println(ground_freq + "," + est_freq + " " + long_id);
		}
		System.out.println("Printing frequencies of hot chains...");
		for (String long_id: estimated_heavy_hitters) {
			int ground_freq = 0;
			Integer g = Tree.union_ground_truth.get(long_id);
			if  (g != null) ground_freq = g.intValue();
			int est_freq = freqEstimate(long_id);
			System.out.println(ground_freq + "," + est_freq + " " + long_id);
		} */
	}

	static void computeHeavyHitters(int limit) {
		Set<Integer> callees = call_pairs.get(0);
		// for each possible child of the root
		for (int i = 1; i <= size_v; i++) {
			if (Config.print_progress) {
				int done = (100 * (i-1)) / size_v;
				if (done >= 10) {
					System.out.print("\b\b\b" + done + "%"); System.out.flush();
				} else {
					System.out.print("\b\b\b " + done + "%"); System.out.flush();
				}
			}
			if (callees.contains(i))
				hh(i+"",i,limit,1);
		}
	}
	static void hh(String long_id, int method_id, int limit, int depth) {
		Set<Integer> callees = call_pairs.get(method_id);
		int est_freq = freqEstimate(long_id);
		if (est_freq >= limit) { // a heavy hitter
			estimated_heavy_hitters.add(long_id);
			// if reached max depth, no need to look at children
			if (depth == Config.depth_limit) return;
			// no static chilren; we are done
			if (callees == null) return;
			for (Integer i : callees) 
				hh(long_id+","+i,i,limit,1+depth);
		}
		if (Config.strictMode) return;
		// not a heavy hitter.
		// case 1: frequency is so low that it is very unlikely to be a hh
		if (est_freq < limit*Config.relax_factor) return;
		// case 2: see if any children are hh
		if (depth > Config.depth_limit) return;
		// no static chilren; we are done
		if (callees == null) return;
		boolean has_hh_children = false;
		for (Integer i : callees) {
			String child = long_id+","+i;
			int f = freqEstimate(child);
			if (f < limit) continue;
			// found a hh child
			has_hh_children = true;
			cached_estimates.put(child,f);
			break;
		}
		if (has_hh_children) {
			// treat the current node as a heavy hitter
			estimated_heavy_hitters.add(long_id);
			// if reached max depth, no need to look at children
			if (depth == Config.depth_limit) return;
			for (Integer i : callees) 
				hh(long_id+","+i,i,limit,1+depth);
		}           
	}

	static void parseArgs(String[] args) {
		if (args.length == 0) {
			System.out.println(
				"-d TRACE_DIR\n" + 
				"-p TRUE/FALSE (Specify private/non-private count sketch.)\n" + 
				"-e VALUE (Specify the value of epsilon in logarithmic form, i.e. epsilon = ln(VALUE).)\n" + 
				"-r RELICATION (Default is 1, i.e. no replication.)\n" + 
				"-l DEPTH_LIMIT (Depth limit of call chains. Default is 10.)\n" + 
				"-s (Specify to use strict exploring algorithm. Relaxed one is used by default.)\n" + 
				"-relax-factor VALUE (Relaxing factor to be used in the exploration algorithm. Default is 0.5.)");
			System.exit(1);
		}
		for (int i = 0; i < args.length; i++) {
			switch(args[i]) {
				case "-d":
					dir = args[++i];
					break;
				case "-p":
					Config.pvt = Boolean.parseBoolean(args[++i]);
					break;
				case "-e":
					Config.epsilon = Math.log(Integer.parseInt(args[++i]));
					break;
				case "-r":
					Config.replication = Integer.parseInt(args[++i]);
					break;
				case "-s":
					Config.strictMode = true;
					break;
				case "-l":
					Config.depth_limit = Integer.parseInt(args[++i]);
					break;
				case "-relax-factor":
					Config.relax_factor = Double.parseDouble(args[++i]);
					break;
				default:
					throw new RuntimeException("Unknown option: " + args[i]);
			}
		}
		if (dir == null || dir.equals("")) {
			throw new RuntimeException("Must specify trace dir.");
		}

		app_name = dir.substring(dir.lastIndexOf('/')+1, dir.length());
	}

	static String computeRE() {
		// compute accuracy for nodes that appear in at least one
		// tree; this is not a great metric, as it does not consider
		// nodes that do *not* appear in any trees.
		int L1 = 0;
		int L1_ground = 0;
		for (String long_id : Tree.union_ground_truth.keySet()) {
			int ground_freq = Tree.union_ground_truth.get(long_id);
			int est_freq = freqEstimate(long_id);
			cached_estimates.put(long_id,est_freq);
			L1_ground += ground_freq;
			int delta = ground_freq-est_freq;
			if (delta < 0) delta = -delta;
			L1 += delta;
		}
		return Util.rel(L1,L1_ground);
	}
	static String computeREforEHH() {    
		// compute accuracy for estimated heavy hitters
		int L1 = 0;
		int L1_ground = 0;
		for (String long_id : estimated_heavy_hitters) {
			int ground_freq = 0;
			Integer g = Tree.union_ground_truth.get(long_id);
			if  (g != null) ground_freq = g.intValue();
			int est_freq = freqEstimate(long_id);
			L1_ground += ground_freq;
			int delta = ground_freq-est_freq;
			if (delta < 0) delta = -delta;
			L1 += delta;
		}
		return Util.rel(L1,L1_ground);
	}
	static int freqEstimate(String long_id) {
		// only ground-truth estimates a cached; otherwise we use too
		// much memory for all explored chains
		Integer x = cached_estimates.get(long_id);
		if (x != null) return x.intValue(); 
		int est_freq = global_sketch.estimate(long_id);
		if (est_freq < 0) est_freq = 0;
		if (est_freq > num_users_opt_out) est_freq = num_users_opt_out;
		return est_freq;
	}
}
