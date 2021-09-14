#!/bin/bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


if [[ ( "" != "$MANAGER1"  &&  "" != "$MANAGER2"  &&  "" != "$MANAGER3" ) ]]; then
   echo "Using the 3 box on-prem proxy configuration"
   bash /opt/harness/proxy/scripts/run.sh
else
   echo "Using the single box on-prem proxy configuration"
   bash /opt/harness/proxy/pov_scripts/run.sh
fi
