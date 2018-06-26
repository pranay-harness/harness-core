#!/usr/bin/env bash
echo "This script will prepare the Harness upgrade installer".
printf "\n"
echo "If you are running Harness for the first time, run the first_time_only_install_harness.sh"

zip -r -X harness_disconnected_on_prem_pov_final.zip harness_disconnected_on_prem_pov_final

echo "Final zip file has been created for the Harness upgrade, scp the zip file to the remote machine"