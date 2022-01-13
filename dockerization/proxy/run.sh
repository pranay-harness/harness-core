#!/bin/bash
# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

if [[ ( "" != "$MANAGER1"  &&  "" != "$MANAGER2"  &&  "" != "$MANAGER3" ) ]]; then
   echo "Using the 3 box on-prem proxy configuration"
   bash /opt/harness/proxy/scripts/run.sh
else
   echo "Using the single box on-prem proxy configuration"
   bash /opt/harness/proxy/pov_scripts/run.sh
fi
