##  This project's structure and scripts are based on the work by:

    https://github.com/emmanuel/coreos-skydns-cloudformation

## The cluster runs on AWS EC2 using Cloud Formation. cloud-init includes:

* etcd
* fleet

## Fleet is then used to deploy:

* registry

## Requirements

* Install aws-cli client
* aws client needs to be configured with an AWS credential.

```bash
aws configure
```

* Get AWS ssh private key for the 'coreoscluster01' keypair in s3, and then `ssh-add` it. Alternatively, generate your own key pair and upload it to our AWS account 
(you'll need to refer to this key in the create_stack command below).

## Manage Stacks
```bash
aws/create-stack.sh
aws/describe-stack.sh stackname
aws/update-stack.sh stackname
aws/delete-stack.sh stackname
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
aws/delete-stack.sh stackname
```
