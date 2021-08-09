#!/usr/bin/env bash
echo "This script will prepare the Harness upgrade installer".
printf "\n"
echo "If you are running Harness for the first time, run the first_time_only_install_harness.sh"

tar -cvzf harness_disconnected_on_prem_pov_final.tar.gz harness_disconnected_on_prem_pov_final

echo "Final tar.gz file has been created for the Harness upgrade, scp the tar.gz file to the remote machine"