#!/bin/bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


#Check for and stop if there is existing execution. That is needed in case of a container restart scenario. 

bash ./stop.sh

if [ "$?" -ne 0 ]; then
	exit 1
fi

source ./start.sh 
