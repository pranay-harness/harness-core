#!/bin/bash
export LOGFILE=./connectivity-check.log

# Time in seconds
export CONNECT_TIMEOUT=15
export CONNECT_MAX_TIME=15
export CURL_PROXY=''

# Colors
ColorOff='\033[0m'
BRed='\033[1;31m'
BGreen='\033[1;32m'
BBlue='\033[1;34m'

success=true
logs=''
errLogs=''

printf "\n${Bold}${BBlue}Starting Harness Connectivity Tests:${ColorOff}\n\n"

function test() {
  printf "$1"
  OUTPUT=$(curl -I --verbose -Ss $CURL_PROXY --connect-timeout $CONNECT_TIMEOUT --insecure --max-time $CONNECT_MAX_TIME $2 2>&1)
  logs+=$OUTPUT
  logs+="\n\n"

  if echo "$OUTPUT" | grep -q "HTTP/1.1 200 OK"; then
    printf "${BGreen}OK${ColorOff}.\n"
  else
    printf "${BRed}FAILED${ColorOff}.\n"
    errorLog+=$OUTPUT
    errorLog+="\n\n"
    success=false
  fi
}

#
# Start testing connectivity
#
test " - Test connecting to [Harness API server]...                   " "https://app.harness.io/api/version"
test " - Test connecting to [Harness Delegate download repository]... " "https://app.harness.io/storage/wingswatchers/watcherprod.txt"

# add more `test` checks here, following the format above

#
# Show error log if there was any failure
#
if [ "$success" = false ]; then
  printf "\n\n$errorLog"
  printf "\n${BRed}Failed to establish connection to Harness. Please check your network."
else
  printf "\nAll tests are successful."
fi

printf "\n\n${ColorOff}"

#
# Also write logs into log file
#
echo $logs > $LOGFILE

#
# Exit 1 if there was a failure
#
if [ "$success" = false ]; then
  exit 1
fi
