#!/bin/bash

LIB_DIR=lib
CONF_DIR=conf

CLASSPATH="${CONF_DIR}"
for i in `ls $LIB_DIR` ; do
	CLASSPATH="${CLASSPATH}:${LIB_DIR}/$i"	
done
export CLASSPATH

java de.codesourcery.eve.skills.ui.Main
