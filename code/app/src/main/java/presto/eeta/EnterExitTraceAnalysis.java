package presto.eeta;

import java.util.*;
import java.io.*;

import presto.Util;
import presto.Config;

class EnterExitTraceAnalysis {
	// directory with the raw data
	public static String dir;
	// number of users in the raw data (accounting for replication)
	public static int num_users_opt_out;
	// size of the set V of static methods
	public static int size_v;
	// list of all trace file names
	public static ArrayList<String> list;
	// number of unique sub-traces in all trees
	public static int num_st_opt_in;
	public static int num_st_opt_out;
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
	// output file path
	public static String output_path;

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
		// conter representing how many times each trace appears in opt-in/out set
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
		int global_max_length_opt_in = 0;
		for (int i = 0; i < list.size(); i++) {
			if (opt_in_trace_ctr[i] == 0) continue;
			String fileName = list.get(i);
			List<String> trace = Util.readOneTrace(dir + "/" + fileName);
			for (int j = 0; j < opt_in_trace_ctr[i]; j++) {
				Tree t = new Tree(); t.addTrace(trace);
				if (global_max_length_opt_in < t.max_length) global_max_length_opt_in = t.max_length;
			}
		}
		num_st_opt_in = Tree.union_sub_traces.size();
		System.out.println("done. [total enter/exit traces: " + num_st_opt_in +
						   ", v: " + size_v + "]");

		// building sketches while building trees for opt out, to save memory
		// build tree sketches and from them the global sketch
		Sketch.init(num_st_opt_in);
		global_sketch = new Sketch();
		System.out.println("*** Building global sketch [t=" + Sketch.t +
						 ",m=" + Sketch.m + ",log(m)="+Sketch.log_m + ",epsilon=" + Sketch.epsilon + "] ...    ");
		System.out.print("*** Reading traces and building trees for opt-out, num_users:" + num_users_opt_out + " ...    ");
		System.out.flush();
		Tree.union_ground_truth = new HashMap<>();
		Tree.union_sub_traces = new HashMap<>();
		int global_max_length_opt_out = 0;
		int num_users_done = 0;
		for (int i = 0; i < list.size(); i++) {
			if (opt_out_trace_ctr[i] == 0) continue;
			String fileName = list.get(i);
			List<String> trace = Util.readOneTrace(dir + "/" + fileName);
			for (int j = 0; j < opt_out_trace_ctr[i]; j++) {
				Tree t = new Tree(); t.addTrace(trace);
				if (global_max_length_opt_out < t.max_length) global_max_length_opt_out = t.max_length;
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
		// Map<String,Integer> union_ground_truth_opt_out= Tree.union_ground_truth;
		num_st_opt_out = Tree.union_sub_traces.size();
		System.out.println("\b\b\b 100% done. [total enter/exit traces: " + num_st_opt_out + 
							", max_length: " + global_max_length_opt_out + 
							", v: " + size_v + "]");
		/* System.out.println(app_name +
							", ratio: " + ((double)num_st_opt_in)/num_st_opt_out);*/
		// no need for list anymore; release the memory
		list.clear();
		String RE = computeRE(); // for ground-truth sub-traces
		/* System.out.println(app_name +
						   ", RE:" + RE);
		System.exit(0); */
		// real heavy hitters
		int limit = (Config.hh_cutoff * num_users_opt_out)/100 ;
		for (String long_id: Tree.union_sub_traces.keySet()) {
			int ground_freq = Tree.union_sub_traces.get(long_id);
			if (ground_freq >= limit) real_heavy_hitters.add(long_id);
		}
		// estimated heavy hitters
		System.out.print("*** Computing heavy hitters, length_limit=" + Config.length_limit + 
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
							"  ground_freq: " + Tree.union_sub_traces.getOrDefault(s, 0) + 
							" estimated_req: " + freqEstimate(s));
			}
		}

		String REforEHH = computeREforEHH(); // for estimated heavy hitters
		// print final metrics
		System.out.println("*** Enter/exit-trace Analysis Result for '" + app_name + "' ***");
		String result_str = "";
		result_str += "Error All: " + RE + "\n";
		result_str += "Error Hot: " + REforEHH + "\n";
		result_str += "Recall: " + recall + "\n";
		result_str += "Precision: " + precision + "\n";
		System.out.print(result_str);

		if (output_path != null) {
			System.out.println("*** Saving result to " + output_path + " ...");
			Util.saveResultToFile(result_str, output_path);
		}
	}

	static void computeHeavyHitters(int limit) {
		Set<Integer> callees = call_pairs.get(0);
		Stack<Integer> call_chain = new Stack<>();
		call_chain.push(0);
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
			if (callees.contains(i)) {
				call_chain.push(i);
				hh("0,"+i,call_chain,limit,1);
				Integer poped = call_chain.pop();
				assert poped == i;
				assert call_chain.size() == 1 && call_chain.peek() == 0;
			}
		}
	}
	static void hh(String st, Stack<Integer> call_chain, int limit, int length) {
		Set<Integer> callees = call_pairs.get(call_chain.peek());
		boolean is_hh = false;
		int est_freq = freqEstimate(st);
		if (est_freq >= limit) is_hh = true;
		else if (!Config.strictMode) {
			// not a heavy hitter in strict mode, see if it is in relaxed mode.
			if (est_freq >= limit*Config.relax_factor && length < Config.length_limit) {
				// see if any children are hh
				// first go further down the call-chain
				boolean has_hh_children = false;
				if (callees != null) {
					for (Integer i: callees) {
						String child = st + "," + i;
						int f = freqEstimate(child);
						if (f < limit) continue;
						// found a hh child
						has_hh_children = true;
						cached_estimates.put(child,f);
						break;
					}
				}
				// second got back if no child found yet
				if (!has_hh_children && call_chain.size() > 1) {
					String child = st + ",-" + call_chain.peek();
					int f = freqEstimate(child);
					if (f >= limit) {
						has_hh_children = true;
						cached_estimates.put(child,f);
					}
				}
				if (has_hh_children) is_hh = true;
			}
		}
		// not a hh
		if (!is_hh) return;
		// is a hh
		estimated_heavy_hitters.add(st);
		// if reached max depth, no need to look at children
		if (length == Config.length_limit) return;
		// first explore further down the call-chain
		if (callees != null) {
			for (Integer i: callees) {
				call_chain.push(i);
				hh(st+","+i, call_chain, limit, length+1);
				Integer poped = call_chain.pop();
				assert poped == i;
			}
		}
		// second go upwards the call-chain
		if (call_chain.size() > 1) {
			Integer i = call_chain.pop();
			assert i != 0;
			hh(st+",-"+i, call_chain, limit, length+1);
			call_chain.push(i);
		}
	}

	static void parseArgs(String[] args) {
		if (args.length == 0) {
			System.out.println(
				"-d TRACE_DIR\n" + 
				"-p TRUE/FALSE (Specify private/non-private count sketch.)\n" + 
				"-e VALUE (Specify the value of epsilon in logarithmic form, i.e. epsilon = ln(VALUE).)\n" + 
				"-r RELICATION (Default is 1, i.e. no replication.)\n" + 
				"-dl DEPTH_LIMIT (Depth limit of stack when parsing the enter/exit traces. Default is 10.)\n" + 
				"-ll LENGTH_LIMIT (Length limit of enter/exit traces. Default is 20.)\n" + 
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
				case "-ll":
					Config.length_limit = Integer.parseInt(args[++i]);
					break;
				case "-dl":
					Config.depth_limit = Integer.parseInt(args[++i]);
					break;
				case "-relax-factor":
					Config.relax_factor = Double.parseDouble(args[++i]);
					break;
				case "-o":
					output_path = args[++i];
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
		for (String long_id: Tree.union_sub_traces.keySet()) {
			int ground_freq = Tree.union_sub_traces.get(long_id);
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
		for (String long_id: estimated_heavy_hitters) {
			int ground_freq = 0;
			Integer g = Tree.union_sub_traces.get(long_id);
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
