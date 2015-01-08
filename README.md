![Stax](http://www.soul-patrol.com/funk/images/stax.jpg)

# Stax

Create stacks (aka stax) on AWS ([Amazon Web Services](aws.amazon.com)) in a private VPC.

## The stax project is based on work from

    https://github.com/emmanuel/coreos-skydns-cloudformation
    https://github.com/philcryer/coreos-aws-cloudformation
    https://github.com/kelseyhightower/kubernetes-coreos

## Requirements

* Install [aws-cli](https://github.com/aws/aws-cli) (Universal Command Line Interface for Amazon Web Services) on your client.

```bash
apt-get install awscli  # Debian GNU/Linux, Ubuntu
brew install aws-cli    # Apple OS X (via Homebrew)
yum install awscli      # Red Hat Enterprise Linux (RHEL), Amazon Linux, Centos
```

* Configure the aws client with your AWS credentials.

```bash
aws configure
```

You will be prompted to enter your AWS access key and secret access key (this will write and store them in ~/.aws/credentials)

* In the AWS Console, create a new Key Pair 

```
* AWS Console > EC2 > Netork & Security > Key Pairs > Create Key Pair
* Name it whatever you want, click 'Create'
```

Download the key and put it in ~/.ssh

```bash
cp YOUR_KEY.pem ~/.ssh
```

* Install [curl](), if needed (OS X has it by default, some Linux distros have it by default) to talk to the web

```bash
apt-get install curl   # Debian GNU/Linux, Ubuntu ???maybe test this
yum install curl       # Red Hat Enterprise Linux (RHEL), Amazon Linux, Centos ???test this
```

## Stax runs [CoreOS](https://coreos.com/) instances on AWS EC2

Configuration is handled by Cloud Formation, services installed via cloud-init are

* etcd
* fleet

## Usage

```bash
  create            Create a new VPC stax on AWS
  describe | desc   Describe the created VPC stax
  connect           Connect to the jumpbox in the VPC over SSH
  list              List all currently built and running stax
  destroy           Destroy existing stax
```

## Create cluster

* Copy the config file, and edit it to suit your environment. Notice there's a cluster size, start with '2' for now.

```bash
cp config-vpc.json.example config-vpc.json
vi config-vpc.json
```

* Run it

```bash
./stax create
```
Watch for any errors.

## Access cluster

Depending on the size of your cluster, it could take 10-15 minutes to build, you can check the status in the AWS console, or try to connect.

* To connect

```bash
./stax connect
```

It will see if the cluster is complete, and if it is, it will populate the jumpbox with needed information and connect you.

* Once connected, get to random/available Docker host with the dockconnect command.

```bash
dockconnect
```

* Once on a Docker host see if fleet can see the other Docker hosts
```bash
fleetctl list-machines
```

* example output
```bash
[ec2-user@ip-10-183-1-99 ~]$ ssh 10.183.2.219 -l core -i ~/.ssh/pccrye-20141005.pem
CoreOS (alpha)
core@ip-10-183-2-219 ~ $
core@ip-10-183-2-219 ~ $ fleetctl list-machines
MACHINE   IP    METADATA
057d212e... 10.183.0.16 -
4f622c15... 10.183.2.219  -
```

Ta... da.

## Tear it down

This will delete the stax, as well as the cloudformation scripts, security groups, etc that it created.

```bash
stax destroy
```
