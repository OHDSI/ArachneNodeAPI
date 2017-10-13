FROM hub.arachnenetwork.com/datanode_env
MAINTAINER alexandr.ryabokon@odysseusinc.com
USER root
VOLUME /tmp
ADD datanode-exec.jar /datanode.jar
ADD run.sh /bin/run.sh
RUN chmod +x /bin/run.sh
EXPOSE 8080
CMD ["/bin/run.sh"]