
Docker build contains the Database, and nodejs server.

Linux (ubuntu):
1.) Install docker
    $ ./install_docker.sh
2.) Build docker image:
    $ sudo docker build -f Dockerfile . -t node:server 
3.) Run server:
    $ sudo docker run -P --name nodejs_server backend node app.js
4.) Done?
    $ sudo docker kill nodejs_server

Mac OSX:
1.) Download docker:
    -https://store.docker.com/editions/community/docker-ce-desktop-mac?tab=description
2.) Install: Seriously? 
3.) Same as above. May or may not need sudo.

Winows(10):
1.) Download docker:
    -https://store.docker.com/editions/community/docker-ce-desktop-windows?tab=description
2.) Install:
    -Seriously?
3.) Run:
    -Probably double click something.
    - http://www.google.com

Windows ver. < 10:
This: https://docs.docker.com/toolbox/toolbox_install_windows/


