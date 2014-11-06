#!/bin/bash

SCRIPT_PATH=$( cd $(dirname $0) ; pwd -P )

STACK_NAME=${1}
#[[ -z "$STACK_NAME" ]] && [[ ! -f '~/.stax/stack_name' ]] && echo "Stack name is required" && exit 1
[[ -f "~/.stax/stack_name" ]] && echo "Stack name is required" && exit 1

STACK_NAME=`cat ~/.stax/stack_name`
echo "Checking stack: $STACK_NAME"

# Describe named stack
aws cloudformation describe-stacks --stack-name $STACK_NAME --output table
