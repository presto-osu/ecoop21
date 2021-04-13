package presto.eeta;

import presto.SketchBase;
import presto.Config;

public class Sketch extends SketchBase {
	public Sketch() {
		super();
	}
	// for enter/exit trace analysis
	public Sketch(Tree tree) {
		init();
		for (String long_id : tree.sub_traces) {
			total_events++;
			for (int j = 0; j < t; j++) {
				int[] res = sha256(long_id,j);
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
