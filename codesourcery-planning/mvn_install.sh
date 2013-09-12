#!/bin/bash

mvn install:install-file -Dfile=lib/jgrapht-jdk1.6.jar -DgroupId=org.jgrapht -DartifactId=jgrapht-jdk16 -Dversion=0.8.0 -DgeneratePom=true -DcreateChecksum=true -Dpackaging=jar
