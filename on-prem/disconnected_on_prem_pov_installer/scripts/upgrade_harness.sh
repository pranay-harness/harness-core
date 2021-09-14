#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

echo "This script will prepare the Harness upgrade installer".
printf "\n"
echo "If you are running Harness for the first time, run the first_time_only_install_harness.sh"

tar -cvzf harness_disconnected_on_prem_pov_final.tar.gz harness_disconnected_on_prem_pov_final

echo "Final tar.gz file has been created for the Harness upgrade, scp the tar.gz file to the remote machine"
