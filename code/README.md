This is the guide for building and running the artifact, and reproducing the experimental results described in the paper.

## Code Structure

The randomization is implemented in Java, and is organized as a [Gradle](https://gradle.org/) project.

- Folder `app` contains the code for both call-chain and enter/exit-trace analysis.
- `run.sh` is a start-up script for running the experiments.
- `plot.py` is a Python script for plotting the results.
- Other files/folders are for Gradle.


## Prerequisites

- Java 1.8+
- Python 3 with [matplotlib](https://matplotlib.org/) and [numpy](https://numpy.org/) for plotting
- Unix-like OS


## Run

### Clone the repository

```bash
$ git clone https://github.com/presto-osu/ecoop21.git
$ cd ecoop21
```

### Download the dataset
Download and extract [traces.tar.gz](https://github.com/presto-osu/ecoop21/releases/download/dataset/traces.tar.gz) to `ecoop21/`:

```bash
$ wget https://github.com/presto-osu/ecoop21/releases/download/dataset/traces.tar.gz
$ tar -xzvf traces.tar.gz
```

The dataset is in the directory named `traces`. For each app evaluated in the paper, there's a sub-directory in it which contains 1000 low-level traces for 1000 users simulated using monkey. Each low-level trace is a sequence of "Enter" and "eXit" events for methods, where each method is denoted by a unique ID. Our experiments are conducted using these low-level traces. Specifically, the traces referred to by the paper, i.e., call chains and enter/exit traces, are extracted from the low-level traces. Besides the low-level traces, there're also three other files in each sub-directory: `callpairs` contains the calling relationship between methods; `v` contains the number of methods of the app; and `list` contains the name of traces.


### Build
Before running the experiments, make sure you build the project first using the following commands:

```bash
$ cd code
$ ./gradlew :app:shadowJar
```

### Usage
Instead of running the analyses by invoking the `java` command, we provide a wrapping script `run.sh` for convinience. The instructions for reproducing the results are based on this script. Here's the description about how to use the script.

| Flag | Description |
|------|--------------|
| `-a TYPE` | Required. Analysis type: `cca` for call-chain analysis, `eeta` for enter/exit-trace analysis.|
| `-n APP_NAME` | Required. Name of app. Must be one of the apps in directory `../traces/`.|
| `-r VALUE` | Optional. # of replication per user. E.g., `-r 10` means 10000 users. Default is 1, i.e., 1000 users.|
| `-e VALUE` | Optional. Value for the privacy parameter. Will use the natural log of this value, i.e., `ε=ln(VALUE)`. E.g., `-e 49` sets ε to ln(49). Default is 9.|
| `-s` | Optional. To use the strict algorithm. The relaxed algorithm is used by default.| 
| `--runs VALUE` | Optional. If specified, the experiment will be repeated for `VALUE` times. To get the confidence intervals, the experiments should be repeated for at least 30 times. Default is 1.|

The script prints the result, including `error-all`, `error-hot`, `recall`, and `precision`, to stdout. It also saves the result to a file in `results/`.

**Example:**
The following command runs the call-chain analysis on `drumpads` for 1000 users, using the relaxed algorithm and `ε=ln(49)`:

```bash
$ bash run.sh -n drumpads -a cca -e 49
```


## Reproducing The Results

Note that to get the complete figures in Section 5 of the paper for all 15 apps and the confidence intervals (the little caps in the bar charts as shown in the paper), you need to run the experiments for 30 runs, which is expected to take more than a month. It is more practical to pick a few fast apps and run a small number of runs or just one run. In this case, the resulting figures will show blanks for some apps and the confidence intervals will be missing.

### Run the randomization

The following commands will generate experimental data for 2 runs of `speedlogic`. You may change the option `-n speedlogic` to other apps for all of the commands to get the data for them. Change the option `--runs 2` to run more rounds if necessary.

We suggest a few fast apps: `speedlogic`, `loctracker`, `parking`, `drumpads`, `equibase`, and `moonphases`.

```bash
# call chain analysis, 1000 users (Fig. 2a & 3a & 5a)
$ bash run.sh -n speedlogic --runs 2 -a cca

# enter/exit analysis, 1000 users (Fig. 2b & 3b & 5b)
$ bash run.sh -n speedlogic --runs 2 -a eeta

# call chain analysis, 10000 users (Fig. 2a & 3a & 5a)
$ bash run.sh -n speedlogic --runs 2 -a cca -r 10

# enter/exit analysis, 10000 users (Fig. 2b & 3b & 5b)
$ bash run.sh -n speedlogic --runs 2 -a eeta -r 10

# strict algorithm (Fig. 4)
$ bash run.sh -n speedlogic --runs 2 -a cca -s
$ bash run.sh -n speedlogic --runs 2 -a eeta -s

# compare different e values (ln3 and ln49) versus the default (ln9) (Fig 6)
$ bash run.sh -n speedlogic --runs 2 -a cca -e 3
$ bash run.sh -n speedlogic --runs 2 -a cca -e 49
$ bash run.sh -n speedlogic --runs 2 -a eeta -e 3
$ bash run.sh -n speedlogic --runs 2 -a eeta -e 49
```

### Plotting the results

Running the experiments above saves the results into directory `results/`. The plotting script read and plot the data in this folder. If there isn't enough data for 30 runs, the confidence intervals will be missing in the figures.

Execute the following command and the figures will pop up and be saved automatically in PDF format:

```bash
$ python3 plot.py
```


