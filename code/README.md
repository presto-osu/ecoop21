This directory contains the code for randomization described in the paper.


## Prerequisites

- Java 1.8+


## Run

#### Clone the repository

```bash
$ git clone https://github.com/presto-osu/ecoop21.git
$ cd ecoop21
```

#### Download The Dataset
Download and extract [dataset.tar.gz](https://github.com/presto-osu/ecoop21/releases/download/dataset/traces.tar.gz) to `ecoop21`. There will be a directory named `traces` containing the raw traces for all apps evaluated in the paper. The experiments below will extract traces (call chains and enter/exit traces) from the raw traces.

#### Build
```bash
$ cd code
$ ./gradlew :app:shadowJar
```

#### Reproduce The Experimental Results

```bash
# call chain analysis, 1000 users (Sec. 5.2 & 5.3)
$ bash runall.sh -a cca

# enter/exit analysis, 1000 users (Sec. 5.2 & 5.3)
$ bash runall.sh -a eeta

# call chain analysis, 10000 users (Sec. 5.2 & 5.3)
$ bash runall.sh -a cca -r 10

# enter/exit analysis, 10000 users (Sec. 5.2 & 5.3)
$ bash runall.sh -a eeta -r 10

# strict algorithm (Sec. 5.3)
$ bash runall.sh -a cca -s
$ bash runall.sh -a eeta -s

# compare different epsilon values(ln3 and ln49) versus the default(ln9) (Sec. 5.4)
$ bash runall.sh -a cca -e 3
$ bash runall.sh -a eeta -e 3
$ bash runall.sh -a cca -e 49
$ bash runall.sh -a eeta -e 49
```

The results: `error-all`, `error-hot`, `recall`, and `precision`, are printed to stdout.

Each experiment of 1000 users(10000 users) is expected to take more than 2 hours(more than 1 day) to complete for all apps together. Press `Ctrl+c` to skip some apps as you want.
