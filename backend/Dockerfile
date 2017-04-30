FROM ubuntu:16.04
COPY docker_initscript.sh /tmp/
COPY node /tmp/
WORKDIR /tmp/
EXPOSE 80
EXPOSE 3000
EXPOSE 5432
RUN sh docker_initscript.sh