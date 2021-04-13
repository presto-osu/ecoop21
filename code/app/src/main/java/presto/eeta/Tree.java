package presto.eeta;

import java.util.*;

import presto.Config;

public class Tree {
	// artificial root
	public Node root;
	// counting the root
	public int size = 1; 
	// all nodes excluding the root
	public Set<Node> all_nodes = new HashSet<Node>();
	// the max depth of a tree node; will always be <= Cofig.depth_limit
	public int max_depth = 0;
	// the max length of an enter/exit traces; will always be <= Config.length_limit;
	public int max_length = 0;
	// temporary pointer used when building the tree from the trace
	private Node curr;
	// each node has a "long id" - a string of comma-separated method
	// ids for the chain. the ground truth maps each long id to frequency across all trees
	public static Map<String,Integer> union_ground_truth = new HashMap<String,Integer>();
	// 
	public static Map<String,Integer> union_sub_traces = new HashMap<String,Integer>();
	public Set<String> sub_traces = new HashSet<>();
	// start with just the root
	public Tree() { root = new Node(this); root.id = 0; curr = root; }
	// add nodes based on a trace of Enter and Exit events
	public void addTrace(List<String> trace) {
		curr = root;
		String valid = "0";
		int valid_len = 0;
		Stack<String> stack = new Stack<String>();
		for (int i = 0; i < trace.size(); i++) {
			String event = trace.get(i);
			int id = Integer.parseInt(event.substring(2));
			if (event.startsWith("X-") && stack.isEmpty())
				throw new RuntimeException("Exit on empty stack; trace " + i);
			if (event.startsWith("E-")) {
				stack.push(event);
				if (stack.size() <= Config.depth_limit) {
					curr = curr.addChild(id);
					if (valid_len < 2*Config.depth_limit) {
						valid += "," + id;
						valid_len++;
						Integer cnt = union_sub_traces.getOrDefault(valid, 0);
						union_sub_traces.put(valid, cnt+1);
						sub_traces.add(valid);
					}
				}
			}
			if (event.startsWith("X-")) {
				String top_event = stack.pop();
				if (top_event.equals("E-"+id)) {
					if (stack.size() < Config.depth_limit) {
						curr = curr.parent;
						if (valid_len < 2*Config.depth_limit) {
							valid += ",-" + id;
							valid_len++;
							Integer cnt = union_sub_traces.getOrDefault(valid, 0);
							union_sub_traces.put(valid, cnt+1);
							sub_traces.add(valid);
						}
						if (curr == root) {
							if (valid_len > max_length) max_length = valid_len;
							valid = "0";
							valid_len = 0;
						}
					}
				}
				else throw new RuntimeException("Expected E-"+id + " but observed " + top_event);
			}
		}
		if (all_nodes.size() + 1 != size) throw new RuntimeException();
	}
}

class Node {
	// method id
	public int id;
	// number of ancestors, including the root
	public int depth;
	// parent node
	public Node parent = null;
	// tree containing this node
	public Tree tree;
	// comma-separated list of method ids, starting with a child of
	// the root and ending with "id"
	public String long_id;
	// all chilren of this node
	public ArrayList<Node> children = new ArrayList<Node>();
	public Node(Tree t) { depth = 0; tree = t; long_id = "";}
	public Node(int i, Node p, Tree t) {
		// id 0 is reserved for the root
		if (i == 0) throw new RuntimeException("node with id 0");
		id = i;
		parent = p;
		depth = 1 + p.depth;
		tree = t;
		if (t.max_depth < depth) t.max_depth = depth;
		t.size++;
		t.all_nodes.add(this);
		// child of the root
		if (depth == 1) long_id = id+"";
		// deeper than child of the root
		else long_id = parent.long_id + "," + id;

		Integer fr = Tree.union_ground_truth.get(long_id);
		if (fr == null) {
			Tree.union_ground_truth.put(long_id,1);
		} else {
			Tree.union_ground_truth.put(long_id,1+fr.intValue());
		}
	}
	public Node addChild(int i) {
		Node ch = null;
		int pos = 0;
		boolean found = false;
		for (; pos < children.size(); pos++) {
			ch = children.get(pos);
			if (ch.id == i) { found = true; break; }
		}
		if (found) return ch;
		ch = new Node(i,this,this.tree);
		children.add(ch);
		return ch;
	}
	public String print(int d) {
		String res = "";
		for (int i = 0; i < depth; i++) res += "  ";
		res += "(" + id + ")";
		res += " [ch:" + children.size() + "][long_id:" + long_id + "]\n";
		if (depth <= d) 
			for (int j = 0; j < children.size(); j++)
				res += children.get(j).print(d);
		return res;
	}
}
