#!/bin/bash

HOSTNAME=`hostname`

echo "Hostname: $HOSTNAME "

CLASSPATH=""
JOPTS=""

if [ "$HOSTNAME" != "tobi" ] ; then
	WFEDIT="."
	CLASSPATH=$WFEDIT:$WFEDIT/build:$WFEDIT/conf
else
	echo "### Using development setup ... ###"
        JOPTS=" -Dcom.sun.management.jmxremote "
	FRAMEWORK=/home/tobi/workspace/stripped_framework
	WFEDIT=/home/tobi/workspace/WfEdit_STABLE
	CLASSPATH=$WFEDIT:$WFEDIT/target/classes:$FRAMEWORK/build:$WFEDIT/conf
fi


for i in `ls $WFEDIT/lib/*.jar` ; do 
	CLASSPATH=$CLASSPATH:$i
done

for i in `ls $WFEDIT/lib/*.zip` ; do 
	CLASSPATH=$CLASSPATH:$i
done

for i in `ls $WFEDIT/plugins/*.zip` ; do 
	CLASSPATH=$CLASSPATH:$i
done

for i in `ls $WFEDIT/plugins/*.jar` ; do 
	CLASSPATH=$CLASSPATH:$i
done

echo "Classpath = $CLASSPATH"
java -Xmx356m -cp $CLASSPATH -Dappconfig=$WFEDIT/conf/wfedit.xml ${JOPTS} $@
