#!/bin/bash

JAR='app/build/libs/app-1.0-SNAPSHOT-all.jar'
TRACE_DIR='../traces/'
EPSILON=9
REPLICATION=1
STRICT=false
NUM_RUNS=1
while [[ $# -gt 0 ]]; do
	key="$1"
	case $key in
		-a|--analysis)
		ANALYSIS=$2
		if [[ $ANALYSIS != "cca" && $ANALYSIS != "eeta" ]]; then
			echo analysis type must be cca or eeta
			exit 1
		fi
		shift
		shift
		;;
		-n|--app)
		APP=$2
		shift
		shift
		;;
		-r|--replication)
		REPLICATION=$2
		shift
		shift
		;;
		-e|--epsilon)
		EPSILON=$2
		shift;
		shift;
		;;
		-s|--strict)
		STRICT=true
		shift
		;;
		--runs)
		NUM_RUNS=$2
		shift
		shift
		;;
	esac
done

if [ "$APP" = '' ]; then
	echo no app specified
	exit 1
fi

if [ "$ANALYSIS" = '' ]; then
	echo no analysis type specified
	exit 1
fi



if [ $ANALYSIS == 'cca' ]; then
	TITLE='Call Chain Analysis'
else
	TITLE='Enter/exit Trace Analysis'
fi

let NUM_USERS=1000*$REPLICATION

for i in $(seq $NUM_RUNS); do
	FILE="$APP-$ANALYSIS-u$NUM_USERS-e$EPSILON-"
	if [ $STRICT = 'true' ]; then
		FILE+="strict"
		echo "========== $TITLE: '$APP', $NUM_USERS users, epsilon=ln$EPSILON, strict, run $i =========="
	else
		FILE+="relaxed"
		echo "========== $TITLE: '$APP', $NUM_USERS users, epsilon=ln$EPSILON, relaxed, run $i =========="
	fi
	CMD="java -cp $JAR presto.$ANALYSIS."
	if [ $ANALYSIS = 'cca' ]; then
		CMD+='CallChainAnalysis'
	else
		CMD+='EnterExitTraceAnalysis'
	fi
	FILE+="-run$i.txt"
	CMD+=" -d $TRACE_DIR$APP -r $REPLICATION -e $EPSILON -o results/$FILE"
	if [ $STRICT = 'true' ]; then
		CMD+=" -s"
	fi
	#echo $CMD
	$CMD
	echo
done
