#!/bin/bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

#Check for and stop if there is existing execution. That is needed in case of a container restart scenario. 

bash ./stop.sh

if [ "$?" -ne 0 ]; then
	exit 1
fi

source ./start.sh 
