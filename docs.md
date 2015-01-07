# Stax

Create cloud stacks (aka stax) on AWS ([Amazon Web Services](aws.amazon.com)) quickly.

## The stax project is based on work from

    https://github.com/emmanuel/coreos-skydns-cloudformation
    and
    https://github.com/philcryer/coreos-aws-cloudformation
    and
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

Download it, placing it in ~/.ssh

* Install [curl](), to talk to the web and [jq](), to parse json output from the awscli

```bash
apt-get install jq curl   # Debian GNU/Linux, Ubuntu ???maybe test this
brew install jq curl      # Apple OS X (via Homebrew)
yum install jq curl       # Red Hat Enterprise Linux (RHEL), Amazon Linux, Centos ???test this
```

## Stax runs [CoreOS](https://coreos.com/) instances on AWS EC2

Configuration is handled by Cloud Formation, services installed via cloud-init are

* etcd
* fleet

## Usage

```bash
stax create     (create a new stack)
stax describe   (describe the stack)
stax update     (update the stack)
stax destroy    (destroy the stack)
```

## Create cluster

### create-vpc

Copy the config file and salt to taste

```bash
cp config-vpc.json.example config-vpc.json
vi config-vpc.json
```

Run it

```bash
./stax create-vpc
```

### create

Copy the config file and salt to taste

```bash
cp config.json.example config.json
vi config.json
```

Run it

```bash
./stax create
```

## Access cluster

### create-vpc 
To access the hosts

* get a PublicIP for the jumpbox from AWS Console ([FIXME] use aws-cli to get this automatically)

* copy the private key to the box ([FIXME] the script needs to handle this)
```bash
scp -i ~/.ssh/the-key-you-specified.pem ~/.ssh/the-key-you-specified.pem ec2-user@PUBLIC-IP:~/.ssh
```

* ssh to the jumpbox
```bash
ssh -i ~/.ssh/the-key-you-specified.pem ec2-user@PUBLIC-IP
```
* now access one of the core nodes to see if Docker can see the other Docker nodes.

```bash
ssh PRIVATE_IP_COREOS_HOST -l core -i ~/.ssh/the-key-you-specified.pem
```

* see if fleet can see the other Docker hosts
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

### create (just the coreOS/docker hosts)
To access the hosts, get a PublicIP for one of the nodes from the AWS console (a work around for now), then ssh to it

```bash
ssh ec2-user@PUBLIC-IP -l core -i ~/.ssh/the-key-you-specified.pem
```

Once there you can see that it can see the other nodes via fleet

```bash
fleetctl list-machines
```

Ta... da.

## Tear it down

This will delete the stax, as well as the cloudformation scripts, security groups, etc that it created.

```bash
stax destroy
```
