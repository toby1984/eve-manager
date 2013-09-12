#!/bin/bash
java -cp /home/tobi/.m2/repository/log4j/log4j/1.2.15/log4j-1.2.15.jar -Dchainsaw.port=4445 org.apache.log4j.chainsaw.Main 2>&1 >/dev/null &
