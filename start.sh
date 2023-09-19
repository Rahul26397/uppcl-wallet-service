#!/bin/bash

PIDFILE=admin.pid

case $1 in
    start)
        # Launch your program as a detached process
        java -Xms8192m -Xmx8192m -jar /u01/deployment/wallet/Wallet-0.0.1-SNAPSHOT.jar > /tmp/wallet.log 2>&1 &
        # Get its PID and store it
        echo $! > ${PIDFILE}
        ;;
    stop)
        kill -15 `cat ${PIDFILE}`
        # Now that it’s killed, don’t forget to remove the PID file
        rm ${PIDFILE}
        ;;
    *)
        echo -n "unknown option. Use either start or stop"
        ;;
esac
