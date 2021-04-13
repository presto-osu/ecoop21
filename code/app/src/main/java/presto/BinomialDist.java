package presto;

import java.util.Random;

public class BinomialDist {
	public static long get(long range, double p) {
		double mean = range * p;
		double variance = range * p * (1 - p);
		double stddev = Math.sqrt(variance);
		double value = getGaussian(mean, stddev);
		return Math.round(value);
	}
	private static Random rand = new Random();
	private static double getGaussian(double mean, double stddev) {
		return mean + rand.nextGaussian() * stddev;
	}
}
