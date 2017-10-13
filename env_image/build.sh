#!/bin/bash

echo "Test docker server"
if docker version
then
	echo "SUCCESS"
else
	echo "Docker Server is not accessable."
	echo "SET DOCKER_HOST"
	exit 1
fi

echo "Removing image if exists"
if docker rmi -f hub.arachnenetwork.com/datanode_env
then
    echo "REMOVED"
else
    echo "NOT EXISTS"
fi

echo "Building docker image"
if docker build -t hub.arachnenetwork.com/datanode_env .
then
    echo "SUCCESS"
else
    echo "SOME PROBLEMS WAS HERE UNTIL IMAGE WAS CREATING"
    exit 1
fi

echo "loggin to docker registry"
if docker login -u pusher -p pusher hub.arachnenetwork.com
then
    echo "LOGIN SUCCESS"
else
    echo "SOME PROBLEMS WAS HERE UNTIL CLIENT WAS LOGGING IN"
    exit 1
fi
echo "Pushing docker image"
if docker push hub.arachnenetwork.com/datanode_env
    then
        echo "SUCCESS"
    else
        echo "SOME PROBLEMS WAS HERE UNTIL IMAGE WAS PUSHING"
        exit 1
fi
exit 0