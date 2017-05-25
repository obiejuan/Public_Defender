docker run --rm -p 3000:3000 --tty -i --name pd_serv -v  $(pwd)/node/:/tmp/ nodejs_serv sh run.sh
