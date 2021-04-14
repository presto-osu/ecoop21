This is the guide for building and running the artifact, and reproducing the experimental results described in the paper.

## Code Structure

- The randomization is implemented in `Java`, and is organized as a `gradle` project.
- The directory `app` contains the code for both call-chain and enter/exit-trace analysis.
- `run.sh` as a start-up script for running the experiments.
- Python script `plot.py` for plotting the figures.


## Prerequisites

- Java 1.8+
- Python 3, matplotlib, numpy
- Unix-like OS


## Run

### Clone the repository

```bash
$ git clone https://github.com/presto-osu/ecoop21.git
$ cd ecoop21
```

### Download the dataset
Download and extract [dataset.tar.gz](https://github.com/presto-osu/ecoop21/releases/download/dataset/traces.tar.gz) to `ecoop21/`. Supposing `dataset.tar.gz` is in `ecoop21\`, use the following command to extract:

```
tar -xzf traces.tar.gz
```

The dataset is in the directory named `traces`. For each app evaluated in the paper, there's a sub-directory in it which contains 1000 low-level traces for 1000 users simulated using monkey. Besides the low-level traces, there're also three other files: `callpairs` contains the calling relationship between methods; `v` contains the number of methods of the app; `list` contains the name of traces. Our randomization experiments are conducted using these low-level traces. The traces referred to by the paper, i.e. call chains and enter/exit traces, are extracted from the low-level traces.


### Build
Before running the experiments below, make sure you build the project first by following the following instructions:

```bash
$ cd code
$ ./gradlew :app:shadowJar
```

### Run
Instead of running the experiments by invoking the java command, we provide a wrapping script `run.sh` for convinience. The instructions for reproducing the results are based on this script. Here's the description of how to use it.

| Flag | Description |
|------|--------------|
| -a cca \| eeta| Analysis type. Must be specified. `cca` for call-chain analysis, `eeta` for enter/exit-trace analysis.|
| -n APP_NAME | The name of app. Must be specified. Must be one of the apps in directory `traces\`.|
| -r VALUE | Replication of users. Optional. Default is 1, i.e. 1000 users. E.g. `-r 10` means 10000 users.|
| -e VALUE | Value of the privacy parameter epsilon. Optional. Default is ln(9). E.g. `-e 49` sets the value as ln(49).|
| -s | Optional. To use the strict algorithm. The relaxed algorithm is used by default.| 
| --runs VALUE | Optional. If specified, the experiment will be repeated by VALUE times. To get the confidence interval, the experiment should be repeated 30 times. It runs just once by default.|

The script prints the result: `error-all`, `error-hot`, `recall`, and `precision`, to stdout. It also saves the result to a file in `results\`.

E.g. the following command runs the call-chain analysis on `drumpads` for 1000 users, using the relaxed algorithm and epsilon=ln(49):

```bash
$ bash run.sh -n drumpads -a cca -e 49
```


## Reproduce

To reproduce the figures in Section 5 of the paper, you first need to get the underlying data. The first part of this section gives detailed instructions on how to get the data. The second part shows how to get the figures using the plotting scripts.

Note that to get the complete figures for all 15 apps and the confidence interval (the little caps in the bar charts as shown in the paper), you need to run the experiments for 30 runs, which is expected to take more than a month. It is more practical to pick a few fast apps and run a small number of runs or just one run. In this case, the resulting figures will show blanks for some apps and the confidence interval will be missing.

### Run the randomization

The following commands will generate experimental data for 2 runs of `speedlogic`. Change the option `-n speedlogic` to other apps for all of the commands and repeat to get the data for them. Change the option `--runs 2` to run more rounds if you want.

We suggest a few fast apps: `speedlogic`, `loctracker`, `parking`, `drumpads`, `equibase`, and `moonphases`.

```bash
# call chain analysis, 1000 users (Fig. 2(a) & 3(a) & 5(a))
$ bash run.sh -n speedlogic --runs 2 -a cca

# enter/exit analysis, 1000 users (Fig. 2(b) & 3(b) & 5(b))
$ bash run.sh -n speedlogic --runs 2 -a eeta

# call chain analysis, 10000 users (Fig. 2(a) & 3(a) & 5(a))
$ bash run.sh -n speedlogic --runs 2 -a cca -r 10

# enter/exit analysis, 10000 users (Fig. 2(b) & 3(b) & 5(b))
$ bash run.sh -n speedlogic --runs 2 -a eeta -r 10

# strict algorithm (Fig. 4)
$ bash run.sh -n speedlogic --runs 2 -a cca -s
$ bash run.sh -n speedlogic --runs 2 -a eeta -s

# compare different epsilon values(ln3 and ln49) versus the default(ln9) (Fig 6)
$ bash run.sh -n speedlogic --runs 2 -a cca -e 3
$ bash run.sh -n speedlogic --runs 2 -a cca -e 49
$ bash run.sh -n speedlogic --runs 2 -a eeta -e 3
$ bash run.sh -n speedlogic --runs 2 -a eeta -e 49
```

### Plot the figures

Running the eperiments above saves the results into directory `results\`. The plotting scripts read the data in it and plot the figures. If there isn't enough data for 30 runs, the confidence interval will be missing in the figures.

Call the following command, and the figures will pop up:

```bash
$ python3 plot.py
```


