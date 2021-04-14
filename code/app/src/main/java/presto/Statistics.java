package presto;

import java.util.*;
import java.util.stream.*;
import java.io.*;
import java.text.DecimalFormat;

public class Statistics {
  double[] data;
  int size;

  public Statistics(double[] data) {
    this.data = data;
    size = data.length;
    //    for (double a: data) if (Double.isNaN(a)) size -= 1;
  }

  double getMean() {
    //    return Arrays.stream(data).parallel().average().getAsDouble();
    double sum = 0.0;
    for (double a : data) {
      //      if (Double.isNaN(a)) continue;
      sum += a;
    }
    return sum / size;
  }

  double getMax() {
    //    return Arrays.stream(data).max().getAsDouble();
    double max = 0;
    for (double a : data) {
      if (Double.isNaN(a)) continue;
      max = Math.max(a, max);
    }
    return max;
  }

  double getMin() {
    //    return Arrays.stream(data).min().getAsDouble();
    double min = Double.MAX_VALUE;
    for (double a : data) {
      if (Double.isNaN(a)) continue;
      min = Math.min(a, min);
    }
    return min;
  }

  double getVariance() {
    double mean = getMean();
    double temp = 0;
    for (double a : data) {
      if (Double.isNaN(a)) continue;
      temp += (a - mean) * (a - mean);
    }
    return temp / (size - 1);
  }

  double getStdDev() {
    return Math.sqrt(getVariance());
  }

  public double getMedian() {
    Arrays.sort(data);
    if (data.length % 2 == 0) return (data[(data.length / 2) - 1] + data[data.length / 2]) / 2.0;
    return data[data.length / 2];
  }

  public double getConfidenceInterval95() {
    return getConfidenceInterval(1 - 0.95);
  }

  public double getConfidenceInterval99() {
    return getConfidenceInterval(1 - 0.99);
  }

  public double getConfidenceInterval999() {
    return getConfidenceInterval(1 - 0.999);
  }

  public double getConfidenceInterval(double alpha) {
    return getConfidenceInterval(getStdDev(), size, alpha);
  }

  public static double getConfidenceInterval(double stdev, int size, double alpha) {
    return zscore(1 - alpha / 2) * stdev / Math.sqrt(size);
  }

  static double Z_MAX = 6;

  private static double zscore(double p) {
    double Z_EPSILON = 0.000001; /* Accuracy of z approximation */
    double minz = -Z_MAX;
    double maxz = Z_MAX;
    double zval = 0.0;
    double pval;

    if (p < 0.0 || p > 1.0) {
      return -1;
    }

    while ((maxz - minz) > Z_EPSILON) {
      pval = poz(zval);
      if (pval > p) {
        maxz = zval;
      } else {
        minz = zval;
      }
      zval = (maxz + minz) * 0.5;
    }
    return zval;
  }

  static double poz(double z) {
    double y, x, w;

    if (z == 0.0) {
      x = 0.0;
    } else {
      y = 0.5 * Math.abs(z);
      if (y > (Z_MAX * 0.5)) {
        x = 1.0;
      } else if (y < 1.0) {
        w = y * y;
        x =
            ((((((((0.000124818987 * w - 0.001075204047) * w + 0.005198775019) * w - 0.019198292004)
                                                        * w
                                                    + 0.059054035642)
                                                * w
                                            - 0.151968751364)
                                        * w
                                    + 0.319152932694)
                                * w
                            - 0.531923007300)
                        * w
                    + 0.797884560593)
                * y
                * 2.0;
      } else {
        y -= 2.0;
        x =
            (((((((((((((-0.000045255659 * y + 0.000152529290) * y - 0.000019538132) * y
                                                                                                        - 0.000676904986)
                                                                                                    * y
                                                                                                + 0.001390604284)
                                                                                            * y
                                                                                        - 0.000794620820)
                                                                                    * y
                                                                                - 0.002034254874)
                                                                            * y
                                                                        + 0.006549791214)
                                                                    * y
                                                                - 0.010557625006)
                                                            * y
                                                        + 0.011630447319)
                                                    * y
                                                - 0.009279453341)
                                            * y
                                        + 0.005353579108)
                                    * y
                                - 0.002141268741)
                            * y
                        + 0.000535310849)
                    * y
                + 0.999936657524;
      }
    }
    return z > 0.0 ? ((x + 1.0) * 0.5) : ((1.0 - x) * 0.5);
  }

  public static void main(String[] args) throws FileNotFoundException, IOException {
    double[] data = new double[30];
    for (int i = 0; i < 30; i++) {
      data[i] = Double.parseDouble(args[i]);
    }
    System.out.println(new Statistics(data).getConfidenceInterval95());
  }
}