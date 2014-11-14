# Stax

Create cloud stacks/stax on AWS ([Amazon Web Services](aws.amazon.com)) quickly.

## The stax project is based on work from

    https://github.com/emmanuel/coreos-skydns-cloudformation
    and
    https://github.com/philcryer/coreos-aws-cloudformation
    and
    https://github.com/kelseyhightower/kubernetes-coreos

## Stax runs [CoreOS](https://coreos.com/) instances on AWS EC2

Configuration is handled by Cloud Formation, services installed via cloud-init are

* etcd
* fleet

## Fleet is then used to deploy:

* registry

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

* Get AWS ssh private key for the 'coreoscluster01' keypair in s3, and then `ssh-add` it. Alternatively, use an existing key pair already on AWS, or generate your own on your AWS account
(you'll need to refer to this key in the create_stack command below).

* Install [jq]() to parse json output from the awscli

```bash
apt-get install jq  # Debian GNU/Linux, Ubuntu ???maybe test this
brew install jq    # Apple OS X (via Homebrew)
yum install jq      # Red Hat Enterprise Linux (RHEL), Amazon Linux, Centos ???test this
```

## Manage Stacks
```bash
stax create     (create a new stack)
stax describe   (describe the stack)
stax update     (update the stack)
stax delete     (delete the stack)
```
## Access cluster

Get a public hostname or ip from one of your new instances from the AWS console (todo: with aws cli command line instructions)

By Default, the SSH is only allowed from the IP address that you provisioned the stack. If you use a different machine, go to 
the cosole to change the security group rules for port 22. Group name looks like <stack-name>-CoreOSSecurityGroup-.

* Login to a machine
```bash
ssh -i <key>.pem core@ec2-54-214-201-163.us-west-2.compute.amazonaws.com
```
* Remote access in to a machine

```bash
export FLEETCTL_TUNNEL={resolvable address of one of the cloud instances}
coreos/list_units.sh
```

## Access cluster
You can test some changes to your cloud without needing to destroy and re-create. SCP your file to a host and:

``` bash
sudo /usr/bin/coreos-cloudinit --from-file /tmp/user-data.yml
```
## Tear down
```bash
stax delete
```
