package presto.cca;

import presto.SketchBase;
import presto.Config;

public class Sketch extends SketchBase {
	public Sketch() {
		super();
	}
	// for call chain analysis
	public Sketch(Tree tree) {
		init();
		for (Node n : tree.all_nodes) {
			if (n == tree.root) throw new RuntimeException();
			total_events++;
			for (int j = 0; j < t; j++) {
				int[] res = sha256(n.long_id,j);
				if (Config.pvt) {
					if (res[1] == 1) {
						plus_one_dp[j][res[0]]++;
					} else if (res[1] == -1) {
						minus_one_dp[j][res[0]]++;              
					} else throw new RuntimeException("bad res[1]");
				} else {
					table_dp[j][res[0]] += res[1];
				}
			}
		}
		if (Config.pvt)
			finalize_private();
	}
}
