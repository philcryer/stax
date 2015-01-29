TODO
* update option? (aws supports it, would be useful to do update if you wanted to increase/decrease the cluster size)
* history option? (give create and destroy times with stax names for audit of the client commands)
* don't populate bashrc if we've already done it on that jumper
* get coreos/docker private ips from an aws query cleaner than we are

DONE
* on create make a ~/.stax/buildtime file wth a timestamp
  - see new directory ~/.stax/audit that records built time and stax name


figure out what this is about when starting docker

==> WARNING: It is highly recommended to set GOMAXPROCS higher than 1


and how to disable consul update_check

==> Failed to check for updates: Get https://checkpoint-api.hashicorp.com/v1/check/consul?arch=amd64&os=linux&signature=f81a63f1-8690-b412-0147-fc8bfcede53f&version=0.4.1: x509: failed to load system roots and no roots provided


put ipv6 back in ntp.conf
 restrict [::1]



verify ntpd is running on dockerstart
