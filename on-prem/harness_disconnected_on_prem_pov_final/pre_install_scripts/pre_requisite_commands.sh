#!/bin/bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

bold=$(tput bold)
normal=$(tput sgr0)
echo "Checking whether touch command execution is allowed"

test_file_path=test_file_$$.txt

touch ${test_file_path}

if [[ $? -eq 0 ]]; then
    echo "✓ touch command execution is successful"
else
    echo "✘ touch command execution failed please grant user permission to create files"
    exit -1
fi

stderr_file_name="pre-requisite-stderr.$$.tmp"
stdout_file_name="pre-requisite-stdout.$$.tmp"

echo "Running Pre-verification script for on-prem"

function checkCommand() {
   name=$1
   if ! command -v "$name" > /dev/null; then
      echo "✘ Command $name not found, this is required for installation"  | tee -a ${stderr_file_name}
   else
       echo "✓ Command $name is installed"  | tee -a ${stdout_file_name}
   fi
}

required_commands=("curl" "sed")

echo "Checking whether required commands are available"

for command_name in "${required_commands[@]}"
do
   checkCommand ${command_name}
done


os_supported=("rhel" "centos" "ubuntu" "debian")

customer_os_id=$(cat /etc/os-release | grep "^ID=" | sed 's/ID=//' )

echo "Operating System has  ${customer_os_id}"

is_os_supported=false;

for os_name in "${os_supported[@]}"
do
  if echo ${customer_os_id} | grep -q ${os_name}; then
       is_os_supported=true;
  fi
done


if ${is_os_supported}; then
   echo "✓ OS $customer_os_id is supported" | tee -a ${stdout_file_name}
else
   echo "✘ OS $customer_os_id is NOT supported, please contact Harness support team" | tee -a ${stderr_file_name}
fi


echo "Checking whether docker is installed and docker daemon is running"

docker_state=$(systemctl show --property ActiveState docker | sed 's/ActiveState=//')

if [[ "$docker_state" == "active" ]]; then
   echo "✓ Docker daemon is running"   | tee -a ${stdout_file_name}
else
   echo "✘ Docker daemon is not running, please start docker daemon"  | tee -a ${stderr_file_name}
fi

echo "Checking whether ports 7143 to 7153 are available"


for port in $(seq 7143 7153)
do
    if [[ $(netstat -tln --numeric-ports | grep ":$port") ]]; then
       echo "✘ Port ${port} is being used, please make it available for installation"   | tee -a ${stderr_file_name}
    else
        echo "✓ Port ${port} is available"  | tee -a ${stdout_file_name}
    fi
done

echo "Checking whether chmod command execution is allowed"

chmod 666 ${test_file_path}

if [[ $? -eq 0 ]]; then
    echo "✓ chmod command execution is successful" | tee -a ${stdout_file_name}
    rm ${test_file_path}
else
    echo "✘ chmod command failed please grant user permission to execute chmod command" | tee -a ${stderr_file_name}
fi

echo "Checking whether mkdir permission is allowed"

test_dir_path=test_dir_$$

mkdir -p ${test_dir_path}

if [[ $? -eq 0 ]]; then
    echo "✓ mkdir permission is allowed"   | tee -a ${stdout_file_name}
    rm -rf ${test_dir_path}
else
    echo "✘ mkdir command failed please grant user permission to create directory" | tee -a ${stderr_file_name}
fi

if [[ -s ${stderr_file_name} ]]
then
     echo ""
     echo "Few pre-requisite for Harness installation ${bold}failed. ${normal}Please see details in $stderr_file_name. Here are the failed checks:"
     echo "$(cat ${stderr_file_name})"
     echo ""
     echo "${bold}Troubleshooting Guide:${normal} https://harness.atlassian.net/wiki/spaces/CUS/pages/342884387/Disconnected+On+Prem+Docker+based+Installer+troubleshooting"
else
     echo "✓ All pre-requisite for Harness installation passed please see details in $stdout_file_name"
fi
