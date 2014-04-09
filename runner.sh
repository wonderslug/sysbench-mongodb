#!/bin/sh
SUITE=$1

if [ "$SUITE" = "" ]
then
  SUITE="test"
fi

OUTDIR=/data3/db
DATADEV=/dev/sda
JOURDEV=/dev/sdb
LOGDEV=/dev/sdc
MONGO_ROOT=/home/alvin
DBPATH=/data/db
LOGPATH=/data3/db
JOURPATH=/data2/db
OUTDIR=/data3/db
JAVA6=/usr/lib/jvm/java-6-openjdk-amd64
JAVA7=/usr/lib/jvm/java-7-openjdk-amd64
JAVA_VERSION=$JAVA6
SB_EXEC_THREADS=16
RH=32
SYSBENCH_PATH=/home/alvin/alvin-sysbench
MONGO_SYNCDELAY=60

sudo update-alternatives --set java $JAVA_VERSION/jre/bin/java
sudo update-alternatives --set javac $JAVA_VERSION/bin/javac

echo "never" | sudo tee /sys/kernel/mm/transparent_hugepage/enabled
echo "never" | sudo tee /sys/kernel/mm/transparent_hugepage/defrag

for VER in "26-source" "2.4.10"; do
#for VER in "2.6.0-rc3" "2.4.9"; do
#  for RH in 32 64 128; do
#  for SB_EXEC_THREADS in 8 16 32 64; do
  for JAVA_DRIVER in "2.11.2" "2.12.0" ; do
    VARIANT=$JAVA_DRIVER
    killall mongod
    echo "3" | sudo tee /proc/sys/vm/drop_caches
    sudo blockdev --setra $RH $DATADEV
    sudo blockdev --setra $RH $JOURDEV
    sudo blockdev --setra $RH $LOGDEV
    rm -r $JOURPATH/journal
    rm -r $DBPATH/*
    rm $LOGPATH/server.log

    if [ "$JOURPATH" != "$DBPATH" ]
    then
      mkdir -p $JOURPATH/journal
      ln -s $JOURPATH/journal $DBPATH/journal
    fi

    MONGOD=$MONGO_ROOT/mongodb-linux-x86_64-$VER/bin/mongod
    numactl --interleave=all $MONGOD --dbpath $DBPATH --logpath $LOGPATH/server.log --fork --syncdelay $MONGO_SYNCDELAY
    sleep 20

    cd $SYSBENCH_PATH
    rm $SYSBENCH_PATH/src/jmongosysbench*.class
    export CLASSPATH=$SYSBENCH_PATH/mongo-java-driver-$JAVA_DRIVER.jar:$SYSBENCH_PATH/src/.

    sh $SYSBENCH_PATH/run.simple.bash $SB_EXEC_THREADS
    OUT=$OUTDIR/$SUITE/$VER/$VARIANT
    mkdir -p $OUT
    mv $LOGPATH/server.log $OUT/.
    mv $SYSBENCH_PATH/mongoSysbench* $OUT/.
  done
done

