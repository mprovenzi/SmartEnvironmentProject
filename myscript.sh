#!/bin/sh

echo "Running myscript"

cd jade/lib
export CLASSPATH=%CLASSPATH.:jade.jar
cd ..
cd ..
echo "jade.jar@classpath"

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:BayesFusion/jSMILE

echo "\nCompiling ESP"
javac -classpath jade/lib/jade.jar:jade/src -d jade/classes jade/src/smartenvironment/devices/_interface/ESP8266.java

echo "\nCompiling LightAgent"
javac -classpath jade/lib/jade.jar:jade/src:BayesFusion/jSMILE/smile.jar:BayesFusion/jSMILE -d jade/classes jade/src/smartenvironment/LightAgent/LightAgent.java

//echo "\nCompiling PresenceAgent"
//javac -classpath jade/lib/jade.jar:jade/src -d jade/classes jade/src/smartenvironment/PresenceAgent/PresenceAgent.java

//echo "\nCompiling DataBaseAgent"
//javac -classpath jade/lib/jade.jar:jade/src:thingspeak_java/src:thingspeak_java/dist/lib/gson-2.2.4.jar:thingspeak_java/dist/lib/unirest-java-1.3.4-SNAPSHOT-jar-with-dependencies.jar:thingspeak_java/dist/lib/log4j.jar -d jade/classes jade/src/smartenvironment/DataBaseAgent/DataBaseAgent.java

//echo "\nCompiling TestAgent"
//javac -classpath jade/lib/jade.jar:jade/src -d jade/classes jade/src/smartenvironment/TestAgent/TestAgent.java

echo "\nBooting up JADE"
java -classpath jade/lib/jade.jar:jade/classes:thingspeak_java/src:thingspeak_java/dist/lib/gson-2.2.4.jar:thingspeak_java/dist/lib/unirest-java-1.3.4-SNAPSHOT-jar-with-dependencies.jar:thingspeak_java/dist/lib/log4j.jar jade.Boot -gui -agents "db:smartenvironment.DataBaseAgent.DataBaseAgent;luz:smartenvironment.LightAgent.LightAgent;pres:smartenvironment.PresenceAgent.PresenceAgent;esp:smartenvironment.devices._interface.ESP8266"
//java -classpath jade/lib/jade.jar:jade/classes:thingspeak_java/src:thingspeak_java/dist/lib/gson-2.2.4.jar:thingspeak_java/dist/lib/unirest-java-1.3.4-SNAPSHOT-jar-with-dependencies.jar:thingspeak_java/dist/lib/log4j.jar jade.Boot -gui -agents "luz:smartenvironment.LightAgent.LightAgent"
//java -classpath jade/lib/jade.jar:jade/classes:thingspeak_java/src:thingspeak_java/dist/lib/gson-2.2.4.jar:thingspeak_java/dist/lib/unirest-java-1.3.4-SNAPSHOT-jar-with-dependencies.jar:thingspeak_java/dist/lib/log4j.jar jade.Boot -gui -agents "db:smartenvironment.DataBaseAgent.DataBaseAgent;luz:smartenvironment.LightAgent.LightAgent;esp:smartenvironment.devices._interface.ESP8266"
//java -classpath jade/lib/jade.jar:jade/classes:thingspeak_java/src:thingspeak_java/dist/lib/gson-2.2.4.jar:thingspeak_java/dist/lib/unirest-java-1.3.4-SNAPSHOT-jar-with-dependencies.jar:thingspeak_java/dist/lib/log4j.jar jade.Boot -gui -agents "db:smartenvironment.DataBaseAgent.DataBaseAgent;luz:smartenvironment.LightAgent.LightAgent;pres:smartenvironment.PresenceAgent.PresenceAgent"
//java -classpath jade/lib/jade.jar:jade/classes:thingspeak_java/src:thingspeak_java/dist/lib/gson-2.2.4.jar:thingspeak_java/dist/lib/unirest-java-1.3.4-SNAPSHOT-jar-with-dependencies.jar:thingspeak_java/dist/lib/log4j.jar jade.Boot -gui -agents "db:smartenvironment.DataBaseAgent.DataBaseAgent"

//test:
//java -classpath jade/lib/jade.jar:jade/classes: jade.Boot -gui -agents "test:smartenvironment.TestAgent.TestAgent"

//javac -classpath jade/lib/jade.jar:jade/src:thingspeak_java/src:thingspeak_java/dist/lib/gson-2.2.4.jar:thingspeak_java/dist/lib/unirest-java-1.3.4-SNAPSHOT-jar-with-dependencies.jar:thingspeak_java/dist/lib/log4j.jar -d jade/classes jade/src/smartenvironment/DataBaseAgent/DataBaseAgent.java


//directory: home/pi/jade 
//java -cp lib/jade.jar:classes jade.Boot -gui -agents "luz:smartenvironment.LightAgent.LightAgent;pres:smartenvironment.PresenceAgent.PresenceAgent"
//directory: home/pi (terminal default)

//java -classpath jade/lib/jade.jar:jade/classes:thingspeak_java/src:thingspeak_java/dist/lib/gson-2.2.4.jar:thingspeak_java/dist/lib/unirest-java-1.3.4-SNAPSHOT-jar-with-dependencies.jar:thingspeak_java/dist/lib/log4j.jar jade.Boot -gui -agents "db:smartenvironment.DataBaseAgent.DataBaseAgent;luz:smartenvironment.LightAgent.LightAgent;pres:smartenvironment.PresenceAgent.PresenceAgent"
