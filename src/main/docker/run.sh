#! /bin/bash
echo "script started"
service postgresql start
if [ "$ACHILES_STARTUP" = "1" ]
then
    echo "ACHILES WILL START"
	service nginx start
else
	echo "ACHILES WILL NOT START"
fi
echo "DATANODE WILL START"
java -Djava.security.egd=file:/dev/./urandom -jar datanode.jar
exit 0