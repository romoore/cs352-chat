#!/bin/bash

#Location of jar file for WM Browser
JAR_FILE=cs352-chat-1.0.6-SNAPSHOT.jar

# Host for main server
LISTEN_PORT=8765

usage() {
  echo "Usage: `basename $0` [-p PORT]"
  echo "  -p PORT : Use alternate listen port"
}

parseopts() {
  while getopts ":p:" optname 
    do
      case "$optname" in
        "p")
          LISTEN_PORT="$OPTARG"
          echo "Setting port to $LISTEN_PORT"
          ;;
        "?")
          echo "Unknown option $OPTARG"
          ;;
        ":")
          echo "Missing value for option $OPTARG"
          ;;
        *)
          echo "Unknown error has occurred"
          ;;
    esac
  done
  return $OPTIND
}
parseopts "$@"
argstart=$?
shift $(($argstart-1))

if [ $# -ne 1 ]
then
  usage
  exit 1
fi

LOCALUSER="ChanHost"

echo "Launching Chat Server on port $LISTEN_PORT with username \
\"$LOCALUSER\""

java -jar $JAR_FILE $LISTEN $LOCALUSER
