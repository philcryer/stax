TODO
* update option? (aws supports it, would be useful to do update if you wanted to increase/decrease the cluster size)
* history option? (give create and destroy times with stax names for audit of the client commands)
* don't populate bashrc if we've already done it on that jumper
* get coreos/docker private ips from an aws query cleaner than we are

DONE
* on create make a ~/.stax/buildtime file wth a timestamp
  - see new directory ~/.stax/audit that records built time and stax name
