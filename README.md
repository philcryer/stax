# Stax

## About

Create stacks (aka stax) on AWS ([Amazon Web Services](aws.amazon.com)) in a private VPC (Virtual Private Cloud) with failover NAT nodes proxying network traffic to elastic [CoreOS](https://coreos.com/) clusters running Docker. Some of the ideas can be seen in this image.

![AWS high availability NAT](docs/aws-ha-nat.png)

After running `stax create` you'll have the following on Amazon AWS:

* __1 Cloudformation script__ (vpc-) instuctions to AWS on how to build the items below
* __1 VPC instace__ (vpc-vpc-) isolated virtual private cloud network
* __1 EC2 instance__ Jumpbox (jump-) used to connect to the network from the outside (public internet)
* __2 EC2 instances__ NAT (NAT1-, NAT2-) proxy network connections to and from the internal CoreOS hosts to the public internet
* __x EC2 instances__ CoreOS/Docker (docker-) these instances run Docker, by default it will create 2 of these, but that number can be changed in the config.json to be as many as you want. As described, these instances only access the public internet through one of the NAT boxes, a script on the instances constantly monitor the NAT instances to rollover to a secondary if one goes down
* __5 Volumes__ (vol-) disk storage, 8 Gigs each by default
* __3 Security Groups__ (sg-) defining ingress and egress rules for network traffic
* __3 Elastic IPs__ for outside (public) access, and for loadbalancing between available instances
* __5 Network Instaces__ (eni-) allow internal and external network traffic
* __1 Launch Configuration__ documenting how the instances are started

## Requirements

Stax runs, and has been fully tested, on Linux (Debian GNU/Linux 7 and Ubuntu 14.04, but others should work fine) and Apple OS X (tested on 10.10 and 10.9).

### Homebrew (OS X only)

* Install [Homebrew](http://brew.sh/) for OS X, which '...installs the stuff you need that Apple didn’t'. Basically it's a *nix package manager like we have in Linux. It's easy to install vi their setup script:

```
ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
```

Visit their site to learn how to install it manually if you don't trust that line.

### Curl

* Install [curl](http://curl.haxx.se/) if you don't already have it installed (OS X has it by default, and most Linux distros have it by default) on your client.

```bash
apt-get install curl   # Debian GNU/Linux, Ubuntu
yum install curl       # Red Hat Enterprise Linux (RHEL), CentOS, Amazon Linux
```

### jq

* Install [jq](https://stedolan.github.io/jq/), a command-line JSON processor, on your client.

```bash
apt-get install jq  # Debian GNU/Linux, Ubuntu
brew install jq    # Apple OS X ([via Homebrew](http://brew.sh/))
yum install jq      # Red Hat Enterprise Linux (RHEL), CentOS, Amazon Linux
```

### awscli

* Install [aws-cli](https://github.com/aws/aws-cli) (Universal Command Line Interface for Amazon Web Services) on your client.

```bash
apt-get install awscli  # Debian GNU/Linux, Ubuntu
brew install awscli    # Apple OS X ([via Homebrew](http://brew.sh/))
yum install awscli      # Red Hat Enterprise Linux (RHEL), CentOS, Amazon Linux
```

* Configure the aws client with your AWS credentials. Find yours from the [users page](https://console.aws.amazon.com/iam/home#users) on the AWS Console. Choose your username, scroll down to Security Credentials > Access Credentials > Access Keys, and click Manage Access Keys.

```bash
aws configure
```

* You will be prompted to enter your AWS region, it needs this, mine is `us-east-1`, it will write and store that in `~/.aws/credentials`
* You will be prompted to enter your access key and secret access key, copy this from the AWS Console you opened above, then it will it write and store that in ~/.aws/credentials.

## Usage

* To get started, clone stax

```bash
git clone https://github.com/philcryer/stax
cd stax
```

* Copy the example config file

```bash
cp config/config.json.example config/config.json
```

* Configure the config file replacing all instances of [<changeMe] and the "CostCenter" definition (something like 1111-1111-ABC11111 will work for testing). Notice the cluster size option that defines the minimal amount of CoreOS nodes running, start with the default of [3] for now.

```bash
vi config/config.json
```

* Run stax

```bash
$ stax
Usage: stax [OPTIONS] <command>

Options:
  -c,--config=CONFIG   Use file CONFIG rather than config/config.json
  -d,--debug           Turn on verbose messages
  -h,--help            Output this message
  -v,--version         Print name and version information

If an argument is required for a long option, so to the short. Same for
optional arguments.

Commands:
  connect           Connect to the jumpbox in the VPC stax over SSH
  create            Create a new VPC stax in AWS
  describe          Describe the stax created from this host
  desc-auto         Describe the autoscaling groups in the stax
  destroy           Destroy the existing VPC stax
  fleet             Run various fleetctl commands against the fleet cluster
  help              Output this message
  history           View history of recently created/destroyed stax
  list              List all completely built and running stax
  services          List servers that are available to run across a stax
  start <service>   Start a service across the stax cluster
  test              Automated test to exercise functionality of stax

For more help, check the docs: https://github.com/philcryer/stax
```

* Create a stax cluster on AWS

```bash
stax create
```

Watch for any errors.

## Accessing the cluster

* It will take ~5 minutes to build, you can test if it's ready by trying to connect.

```bash
stax connect
```

The command will check if the cluster is built, and if it is, it will populate the jumpbox with needed information and then connect you to it.

* Once on the jumpbox you can get to randomly available CoreOS/Docker host with the dockconnect command.

```bash
dockconnect
```

* Once on a CoreOS host, see if fleet can see the other CoreOS hosts.

```bash
fleetctl list-machines
```

* Example output
```bash
[ec2-user@ip-10-183-1-99 ~]$ dockconnect
CoreOS (alpha)
core@ip-10-183-2-219 ~ $
core@ip-10-183-2-219 ~ $ fleetctl list-machines
MACHINE   IP    METADATA
5203d410... 10.183.2.219  -
5cc5c4cf... 10.183.0.124  -
a0692146... 10.183.0.125  -
```

* Next connect to a running Docker container on that CoreOS system

```bash
docker exec -it `docker ps|tail -n1|cut -d" " -f1` /bin/bash
```

* and make sure the running consul service knows about all of the other CoreOS/Docker instances

```bash
bash-4.3# consul members
```

* Example output
```bash
core@ip-10-183-2-219 ~ $ docker exec -it `docker ps|tail -n1|cut -d" " -f1` /bin/bash
bash-4.3# consul members
Node                          Address            Status  Type    Build  Protocol
ip-10-183-2-219.ec2.internal  10.183.2.219:8301  alive   server  0.4.1  2
ip-10-183-0-124.ec2.internal  10.183.0.124:8301  alive   server  0.4.1  2
ip-10-183-0-125.ec2.internal  10.183.0.125:8301  alive   server  0.4.1  2
```

Ta... da.

## Destroyng the cluster

* To delete the cluster, as well as the cloudformation scripts, security groups, etc that it created.

```bash
stax destroy
```

## Other details

* The stax configuration is handled by Cloud Formation scripts, see them in the templates directory.
* Services installed on CoreOS are etcd and fleet by default.
* Future plans call for use of an orchestrator to run Docker instances across the cluster using fleet (or something else).

## Acknowledgements

The stax project started off with ideas from the following projects:

* [emmanuel/coreos-skydns-cloudformation](https://github.com/emmanuel/coreos-skydns-cloudformation)
* [xueshanf/coreos-aws-cloudformation](https://github.com/xueshanf/coreos-aws-cloudformation)
* [kelseyhightower/kubernetes-coreos](https://github.com/kelseyhightower/kubernetes-coreos)

Thanks everyone, Open Source FTW!

## License

The MIT License (MIT)

Copyright (c) 2015 philcryer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Stax Studio

* Stax, besides being a clever take on the word stacks, is named after the famous Stax Recording Studio in Memphis, TN. If you're ever in Memphis, visit the awesome [Stax Museum](http://www.staxmuseum.com/)... it's far more interesting than Sun Studios, but I digress.

![Stax Museum](https://media-cdn.tripadvisor.com/media/photo-s/01/70/29/68/stax-recording-studio.jpg)
