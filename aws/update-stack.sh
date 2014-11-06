#!/bin/bash

which aws > /dev/null 2>&1 || { echo "aws cli it's not installed.  Aborting."; exit 1; }

# Default values
SCRIPT_PATH=$( cd $(dirname $0) ; pwd -P )
STACK_NAME="CoreOS-test-$RANDOM"
STACK_DISCOVERY_URL=`curl -s https://discovery.etcd.io/new`
STACK_INSTANCE_TYPE="m3.medium"
STACK_CLUSTER_SIZE=3
STACK_KEY_PAIR="coreoscluster01"
STACK_REGION="us-west-2"

dryrun=0

help(){
    echo "update-stack [-s <stack-name>] -k <keypair-name> -t <discovery-toten-url> -r region -t <ec2-type> -n <ec2-number> -d "
}

while getopts "s:k:t:r:n:dh" OPTION
do
    case $OPTION in
        s)
          STACK_NAME==$OPTARG
          ;;
        k)
          STACK_KEY_PAIR=$OPTARG
          ;;
        t)
          STACK_INSTANCE_TYPE=$OPTARG
          ;;
        t)
          STACK_DISCOVERY_URL=$OPTARG
          ;;
        r)
          STACK_REGION=$OPTARG
          ;;
        n)
          STACK_CLUSTER_SIZE=$OPTARG
          ;;
        d)
          dryrun=1
          ;;
        [h?])
          help
          exit
          ;;
    esac
done

aws ec2 describe-key-pairs --key-names $STACK_KEY_PAIR > /dev/null 2>&1 || { echo "Keypair $STACK_KEY_PAIR does not exit."; exit 1; }

echo "Updating CloudFormation Stack with these parameters:"
echo "Name: $STACK_NAME"
echo "Discovery URL: $STACK_DISCOVERY_URL"
echo "Instance Type x Cluster Size: $STACK_INSTANCE_TYPE x $STACK_CLUSTER_SIZE"
echo "EC2 Key Pair: $STACK_KEY_PAIR"

[ $dryrun -eq 1 ] && { echo "Dryrun only."; exit 0; }

aws cloudformation update-stack \
  --region $STACK_REGION \
  --stack-name $STACK_NAME \
  --template-body file://$SCRIPT_PATH/cloudformation-template.json \
  --parameters \
    "ParameterKey=InstanceType,ParameterValue=$STACK_INSTANCE_TYPE,UsePreviousValue=false" \
    "ParameterKey=ClusterSize,ParameterValue=$STACK_CLUSTER_SIZE,UsePreviousValue=false" \
    "ParameterKey=DiscoveryURL,ParameterValue=$STACK_DISCOVERY_URL,UsePreviousValue=false" \
    "ParameterKey=AdvertisedIPAddress,ParameterValue=private,UsePreviousValue=false" \
    "ParameterKey=AllowSSHFrom,ParameterValue=0.0.0.0/0,UsePreviousValue=false" \
    "ParameterKey=KeyPair,ParameterValue=$STACK_KEY_PAIR,UsePreviousValue=false"
