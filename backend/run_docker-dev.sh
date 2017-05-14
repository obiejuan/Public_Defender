sudo docker run --rm --tty -i -p 3000:3000 -p 5432:5432 --name pd_serv -v  $(pwd)/node/:/tmp/ nodejs_serv sh run.sh
