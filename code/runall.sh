#!/bin/bash

APPS=('barometer' 'bible' 'dpm' 'drumpads' 'equibase' 'goforit' 'loctracker' 'mitula' \
    'moonphases' 'parking' 'parrot' 'post' 'quicknews' 'speedlogic' 'vidanta')

JAR='app/build/libs/app-1.0-SNAPSHOT-all.jar'
TRACE_DIR='../traces/'
EPSILON=9
REPLICATION=1
STRICT=false
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
	esac
done

if [ $ANALYSIS == 'cca' ]; then
	TITLE='Call Chain Analysis'
else
	TITLE='Enter/exit Trace Analysis'
fi

let NUM_USERS=1000*$REPLICATION

for APP in ${APPS[@]}; do
	if [ $STRICT = 'true' ]; then
		echo "========== $TITLE: '$APP' $NUM_USERS users epsilon=ln$EPSILON strict =========="
	else
		echo "========== $TITLE: '$APP' $NUM_USERS users epsilon=ln$EPSILON relaxed =========="
	fi
	CMD="java -cp $JAR presto.$ANALYSIS."
	if [ $ANALYSIS = 'cca' ]; then
		CMD+='CallChainAnalysis'
	else
		CMD+='EnterExitTraceAnalysis'
	fi
	CMD+=" -d $TRACE_DIR$APP -r $REPLICATION -e $EPSILON"
	if [ $STRICT = 'true' ]; then
		CMD+=" -s"
	fi
	$CMD
	echo
done