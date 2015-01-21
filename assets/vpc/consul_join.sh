/usr/bin/docker exec -it consul consul join `curl -Ls http://172.17.42.1:4001/v2/machines | sed -e 's|http://\\([.0-9]*\\):[0-9]*,*|\\1|g'`
