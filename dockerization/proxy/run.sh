#!/bin/bash

if [[ ( "" != "$MANAGER1"  &&  "" != "$MANAGER2"  &&  "" != "$MANAGER3" ) ]]; then
   echo "Using the 3 box on-prem proxy configuration"
   bash /opt/harness/proxy/scripts/run.sh
else
   echo "Using the single box on-prem proxy configuration"
   bash /opt/harness/proxy/pov_scripts/run.sh
fi