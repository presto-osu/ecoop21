package presto;

import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public abstract class SketchBase {
	public static int t = Config.t; 
	public static int m; // must be a power of 2    
	public static int log_m; 
	// public static double epsilon = 2.19722457734; // ln(9)
	public static double epsilon = Config.epsilon;
	private static double p;
	private static double scaling;
	private static MessageDigest digest; 
	public static void init(int num_nodes_opt_in) {
		m = (int) Math.round(Math.pow(2,Math.ceil(Math.log(num_nodes_opt_in)/Math.log(2))));
		log_m = (int) Math.round((Math.log(m) / Math.log(2)));  
		if (Config.pvt) {
			double x = Math.exp(epsilon);
			p = x/(1+x);
			scaling = (x+1)/(x-1);
		} else {
			scaling = 1;
		}
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (Exception e) { throw new RuntimeException("SHA-256 died"); }   
	}
	protected int total_events = 0;
	protected int[][] table_dp;
	protected int[][] plus_one_dp;
	protected int[][] minus_one_dp;
	public int estimate(String long_id) {       
		int[] vals = new int[t];
		for (int j = 0; j < t; j++) {
			int[] res = sha256(long_id,j);
			vals[j] = res[1]*table_dp[j][res[0]];               
		}
		Arrays.sort(vals);
		// private; must scale
		if (t %2 == 0)
			return (int) Math.round( scaling * 0.5f * (vals[t/2 - 1] + vals[t/2]));
		return (int) Math.round(scaling * vals[(t-1)/2]);
	}
	public SketchBase() {
		init();
	}
	
	protected int[] sha256(String long_id, int row) {
		byte[] encoded_hash;
		String s = long_id+"|"+row;
		try {
			encoded_hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) { throw new RuntimeException("SHA-256 died for " + s); }
		boolean[] all_bits = new boolean[256];
		int pos = 0;
		for (int i = 0; i < 32; i++) {
			int x = encoded_hash[i] + 128; // value from 0 to 255
			for (int j = 0; j < 8; j++) {
				if (x % 2 == 0)
					all_bits[pos+7-j] = false;
				else
					all_bits[pos+7-j] = true;               
				x /= 2;
			}
			pos += 8;
			if (pos > log_m+1) break;
		}
		// take the first log(m) bits and make an int. this is res[0]. take
		// the next bit and make it the sign. this is res[1]
		int[] res = new int[2];
		res[0] = 0;
		for (int i = 0; i < log_m; i++)
			if (all_bits[i]) res[0] = 2*res[0]+1; else res[0] = 2*res[0];
		if (all_bits[log_m]) res[1] = 1; else res[1] = -1;
		return res;
	}
	protected void init() {
		table_dp = new int[t][m];
		plus_one_dp = new int[t][m];
		minus_one_dp = new int[t][m];            
		// start with zero in all table cells
		for (int i = 0; i < t; i++)
			for (int j = 0; j < m; j++) {
				table_dp[i][j] = 0;
				plus_one_dp[i][j] = 0;
				minus_one_dp[i][j] = 0;         
			}
	}
		
	public void addContributionOf(SketchBase s) {
		for (int i = 0; i < t; i++)
			for (int j = 0; j < m; j++) {
				table_dp[i][j] += s.table_dp[i][j];
			}
	}
	protected void finalize_private() { 
		for (int i = 0; i < t; i++)
			for (int j = 0; j < m; j++) {
				int num_plusone = plus_one_dp[i][j];
				int num_minusone = minus_one_dp[i][j];
				int num_zero = total_events - num_plusone - num_minusone;
				long bin_plusone = 2*BinomialDist.get(num_plusone,p) - num_plusone;
				long bin_minusone = 2*BinomialDist.get(num_minusone,p) - num_minusone;
				long bin_zero = 2*BinomialDist.get(num_zero,0.5) - num_zero;
				long res = bin_plusone - bin_minusone + bin_zero;
				if (res > Integer.MAX_VALUE) throw new RuntimeException();
				if (res < Integer.MIN_VALUE) throw new RuntimeException();
				table_dp[i][j] = (int) res;
			} 
	}
}
