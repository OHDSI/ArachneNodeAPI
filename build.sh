#!/bin/bash

echo "Test docker client"
if docker >> /dev/null
then
	echo "SUCCESS"
else
	echo "Docker is not installed. Exiting"
	exit 1
fi

echo "Test docker server"
if docker version
then
	echo "SUCCESS"
else
	echo "Docker Server is not accessable."
	echo "SET DOCKER_HOST"
	exit 1
fi

echo "Removing container if exists"
if docker stop datanode
then
    docker rm -f datanode
    echo "REMOVED"
else
    echo "NOT EXISTS"
fi

echo "Removing image if exists"
if docker rmi -f arachne/datanode
then
    echo "REMOVED"
else
    echo "NOT EXISTS"
fi

echo "Building docker image"
if mvn clean package -P "$1"
then
    echo "SUCCESS"
else
    echo ""
    exit 1
fi

echo "Creating container"
if docker create --privileged --restart=always -v /datanodedata/postgresql/var/lib:/var/lib/postgresql -v /datanodedata/postgresql/log:/var/log/postgresql -v /datanodedata/postgresql/etc:/etc/postgresql --env "ACHILES_STARTUP=1" --name datanode --net="host" arachne/datanode
then
    echo GOOD WORK! To start container: docker start datanode
else
    echo "SOME PROBLEMS WAS HERE UNTIL CONTAINER WAS CREATING"
    exit 1
fi
exit 0