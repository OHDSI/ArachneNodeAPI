#! /bin/bash
startservices.sh
echo "DATANODE WILL START"
java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dump -Djava.security.egd=file:/dev/./urandom -jar datanode.jar
exit 0
