package presto;

import java.io.*;
import java.util.*;

public class Util {
	public static String rel(int x, int y) { return Config.df.format(((double)x)/y); }

	public static Map<Integer,Set<Integer>> readCallPairs(String path) {
		Map<Integer, Set<Integer>> ret = new HashMap<>();
		File f = new File(path);
		String st;      
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			while ((st = br.readLine()) != null) {
				int x = st.indexOf(",");
				String s1 = st.substring(0,x);
				String s2 = st.substring(x+1);
				int m1 = Integer.parseInt(s1);
				int m2 = Integer.parseInt(s2);
				Set<Integer> callees = ret.get(m1);
				if (callees == null) {
					callees = new HashSet<Integer>();
					ret.put(m1,callees);
				}
				callees.add(m2);
			}
		} catch (IOException e) { throw new RuntimeException(e); }
		return ret;
	}

	public static ArrayList<String> readList(String path) {
		ArrayList<String> ret = new ArrayList<>();
		File f = new File(path);
		String st;      
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			while ((st = br.readLine()) != null) {
				ret.add(st);
			}
		} catch (IOException e) { throw new RuntimeException(e); }
		return ret;
	}

	public static List<String> readOneTrace(String path) {
		File tracef = new File(path);
		ArrayList<String> res = new ArrayList<String>();
		String st;
		try {
			BufferedReader br = new BufferedReader(new FileReader(tracef));
			while ((st = br.readLine()) != null) {
				if (st.startsWith("***")); else res.add(st);
			}
		} catch (IOException e) { throw new RuntimeException(e); }
		return res;
	}

	public static int readSizeV(String path) {
		String st;
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(path)));
			st = br.readLine(); 
			return Integer.parseInt(st);
		} catch (IOException e) { throw new RuntimeException(e); }
	}
}
