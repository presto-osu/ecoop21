package presto;

import java.text.DecimalFormat;

// Config for both analysis
public class Config {
    // The depth limit of call chains, and the stack limit of enter/exit traces
    public static int depth_limit = 10;
    // length limit of enter/exit traces
    public static int length_limit = 20;
    // print messages about the percent progress of long tasks
    public static final boolean print_progress = true;
    // printing format for metrics
    public static final DecimalFormat df = new DecimalFormat("0.000");
    // replicate each real trace this many times
    public static int replication = 1;
    // cutoff for heavy hitters, in percent: >= num_users*cutoff means a hh
    public static final int hh_cutoff = 90;
    // relaxing factor used in the relaxed algorithm for exploring hot traces
    public static double relax_factor = 0.5;

    public static double epsilon = Math.log(9);

    public static int t = 256;
    // private or non-private version of count sketch
    public static boolean pvt = true;

    public static boolean printFalse = false;

    // false if relaxed algorithm is to be used for exploring hot traces
    public static boolean strictMode = false;
}
