FROM ubuntu
MAINTAINER alexandr.ryabokon@odysseusinc.com
RUN apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys B97B0AFCAA1A47F044F244A07FCC7D46ACCC4CF8
RUN echo "deb http://apt.postgresql.org/pub/repos/apt/ precise-pgdg main" > /etc/apt/sources.list.d/pgdg.list
RUN apt-get update && apt-get install -y python-software-properties software-properties-common postgresql-9.6 postgresql-client-9.6 postgresql-contrib-9.6 openjdk-8-jdk nginx
USER postgres
RUN /etc/init.d/postgresql start &&\
    psql --command "CREATE USER ohdsi WITH PASSWORD 'ohdsi';" &&\
    createdb -O ohdsi datanode
RUN echo "local datanode  ohdsi md5" >> /etc/postgresql/9.6/main/pg_hba.conf
#RUN sed -ie 's/^port\s*[=]{1}\s*\d{1,5}$/port = 5433/g' file.txt
RUN echo "port=5433" >> /etc/postgresql/9.6/main/postgresql.conf
VOLUME  ["/etc/postgresql", "/var/log/postgresql", "/var/lib/postgresql"]
RUN /etc/init.d/postgresql stop
USER root
RUN rm /etc/nginx/nginx.conf
RUN rm /etc/nginx/sites-enabled/default
ADD nginx.conf /etc/nginx/nginx.conf
RUN mkdir -p /var/nginx/www
ADD achiles_www.tar.gz /var/nginx/www
RUN chown www-data:www-data -R /var/nginx/www
EXPOSE 8008
# CMD ["/usr/lib/postgresql/9.6/bin/postgres", "-D", "/var/lib/postgresql/9.6/main", "-c", "config_file=/etc/postgresql/9.6/main/postgresql.conf"]

# Build
# docker build -t arachne/datanode_env .

# Run (for developers only)
# docker run -it arachne/datanode_env bash