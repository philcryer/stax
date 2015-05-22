srv_name="dj"

# removes any duplicate services (if they exist)
fleetctl destroy $(fleetctl list-units | grep "^$srv_name")

# loads the service and timer units
fleetctl load $srv_name.timer
fleetctl load $srv_name.service

# starts the timer
fleetctl start $srv_name.timer
