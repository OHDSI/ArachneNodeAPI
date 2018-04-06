#! /bin/bash
startservices.sh
echo "DATANODE WILL START"
java -Djava.security.egd=file:/dev/./urandom -jar datanode.jar
exit 0