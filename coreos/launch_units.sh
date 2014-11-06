#!/bin/bash

if [ -z "$FLEETCTL_TUNNEL" ]; then
    answer='N'
    echo -n "Are you on one of the cluster machines? [Y/N]"
    read answer
    echo ""
    if [ "X$answer" = "XY" ]; then
        echo "Continue."
    else 
        echo "You must set FLEETCTL_TUNNEL (a resolvable address to one of your CoreOS instances)"
        echo "e.g.:"
        echo "export FLEETCTL_TUNNEL=1.2.3.4"
        exit 1
    fi
fi

SCRIPT_PATH=$( cd $(dirname $0) ; pwd -P )
cd $SCRIPT_PATH/../units

# Add the service templates to Fleet
fleetctl submit registry/registry@.service
# Start instantiated units from the templates (+ a number)
fleetctl start registry/registry@1.service
