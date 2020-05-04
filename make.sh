#!/bin/bash

DIRECTORY=$( cd "$(dirname "$0")" ; pwd -P )

javac -d $DIRECTORY/bin $DIRECTORY/src/Beleg_Server.java

javac -d $DIRECTORY/bin $DIRECTORY/src/Beleg_Client.java
