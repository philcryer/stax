# TODO

## PENDING
* update option? (aws supports it, would be useful to do update if you wanted to increase/decrease the cluster size)

* don't populate bashrc if we've already done it on that jumperbox

* get coreos/docker private ips from an aws query cleaner than we are (sometimes we'll miss one IP)

* figure out what this is about when starting docker
==> WARNING: It is highly recommended to set GOMAXPROCS higher than 1

* how to disable consul update_check
==> Failed to check for updates: Get https://checkpoint-api.hashicorp.com/v1/check/consul?arch=amd64&os=linux&signature=f81a63f1-8690-b412-0147-fc8bfcede53f&version=0.4.1: x509: failed to load system roots and no roots provided

* put ipv6 back in ntp.conf::    restrict [::1]
* verify ntpd is running on dockerstart
* ssh keys on CoreOS hosts so CoreOS hosts can all talk and ssh to eachother

* rework some of the sed/awk/cut stuff with jq (json query parser). Example:
⚡  aws cloudformation describe-stacks --stack-name `cat ~/.stax/stax-name` | jq '.Stacks[].CreationTime, .Stacks[].StackId, .Stacks[].StackStatus'
"2015-02-12T18:50:25.488Z"
"arn:aws:cloudformation:us-east-1:499281755213:stack/vpc-stax-30371-perforable/015b1260-b2e8-11e4-b0a5-50e2416294e0"
"CREATE_COMPLETE"
more: https://stedolan.github.io/jq/tutorial/



## DONE
* history option? (give create and destroy times with stax names for audit of the client commands)
* on create make a ~/.stax/buildtime file wth a timestamp
  - see new directory ~/.stax/audit that records built time and stax name
