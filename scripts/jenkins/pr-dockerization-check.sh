#!/bin/bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

function detect_unknown_urls(){
    if ! [[ "$1" =~ "app.harness.io" ]]; then #If curl or wget found then it will only allow app.harness.io
        echo "ERROR $2: Unknown URL Found in line: $line"
        # exit 1
    fi
}

# This will filter out files modified/added inside dockerization folder.
for file in `git diff --name-status --diff-filter=MADR HEAD@{1} HEAD -1`; do

    if [[ ("$file" == "dockerization"* && ${file: -3} == ".sh") ]]; then
        echo "Dockerization check started for file: " $file
        while IFS= read -r line; do

            if  [[ ! ( "$line" =~ ^echo || "$line" =~ ^\# ) ]]; then
                #Searching for variables inside shell script
                variables=$(grep -E '^[[:alnum:]][-|_[:alnum:]]{0,100}\=' <<< $line)
                key=$(echo $variables | awk -F= '{print $1}')
                value=$(echo $variables | awk -F= '{print $2}' | sed -e 's/^"//' -e 's/"$//')

                if [[ ! -z $variables && ! -z $key && ! -z $value ]]; then
                    # This will handle something like this url="https://fake.url.com"
                    # url=$(grep -E '(https|http|ftp|file)://' <<< $value)
                    url="`grep -E '(https|http|ftp|file)://' <<< $value`"
                    if [[ ( ! -z $url ) ]]; then
                        detect_unknown_urls "$line" '1'
                    fi
                fi

                # This will handle something like this curl https://fake.url.com
                # url=$(grep -E '(https|http|ftp|file)://' <<< $line)
                url="`grep -E '(https|http|ftp|file)://' <<< $line`"
                if [[ ( "$line" =~ "curl" || "$line" =~ "wget" ) && ( ! -z $url ) ]]; then
                    detect_unknown_urls "$line" '2'
                fi

            fi
        done < $file
    fi
done
