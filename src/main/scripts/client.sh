#!/bin/bash

#Location of jar file for WM Browser
JAR_FILE=cs352-chat-1.0.7.jar

# Host for main server
SERVER_HOST="localhost"
# Port for main server
SERVER_PORT=8765

# Minimum port number for clients
BASE_PORT=2048
# Max port number for clients
MAX_PORT=$((32768-2048))
# GUI option
GUI=""

usage() {
  echo "Usage: `basename $0` [-g] [-s HOST] [-p PORT] USERNAME"
  echo "  -g : Use graphical interface"
  echo "  -h HOST : Use alternate server hostname"
  echo "  -p PORT : User alternate server port"
}

parseopts() {
  while getopts ":gh:p:" optname 
    do
      case "$optname" in
        "h")
          SERVER_HOST="$OPTARG"
          echo "Setting host to $SERVER_HOST"
          ;;
        "p")
          SERVER_PORT="$OPTARG"
          echo "Setting port to $SERVER_PORT"
          ;;
        "g")
          GUI="--gui"
          echo "Using gui"
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

LOCALUSER=$1

echo "Connecting to Chat Server @ $SERVER_HOST:$SERVER_PORT with username \
\"$LOCALUSER\""

# Pick a random port
PORT=$((RANDOM%$MAX_PORT+$BASE_PORT))

java -jar $JAR_FILE $PORT $LOCALUSER $SERVER_HOST $SERVER_PORT $GUI \
  || echo "Try again, you probably got a bad port number!"
