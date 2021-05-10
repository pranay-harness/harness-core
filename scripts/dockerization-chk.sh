#!/bin/bash

function detect_unknown_urls(){
    if ! [[ "$1" =~ "app.harness.io" ]]; then #If curl or wget found then it will only allow app.harness.io
        echo "ERROR $2: Unknown URL Found in line: $line"
        # exit 1
    fi
}

echo "curl https://storage.googleapis.com/harness-prod-public/public/shared/tools/alpn/release/8.1.13.v20181017/alpn-boot-8.1.13.v20181017.jar  --output alpn-boot-8.1.13.v20181017.jar \
JAVA_OPTS=" -Xbootclasspath/p:alpn-boot-8.1.13.v20181017.jar"" > dockerization-delegate.sh

echo "curl http://\$url" >> dockerization-delegate.sh
echo "url="http://fake.url.com"" >> dockerization-delegate.sh
echo "urls="https://app.harness.io"" >> dockerization-delegate.sh


# for file in `git diff --name-status --diff-filter=MADR HEAD@{1} HEAD -1`; do
file="dockerization-delegate.sh"
    if [[ ("$file" == "dockerization"* && ${file: -3} == ".sh") ]]; then
        echo "Dockerization check started for file: " $file
        while IFS= read -r line; do

            if  [[ ! ( "$line" =~ ^echo || "$line" =~ ^\# ) ]]; then
                variables=$(grep -E '^[[:alnum:]][-|_[:alnum:]]{0,100}\=' <<< $line)
                key=$(echo $variables | awk -F= '{print $1}')
                value=$(echo $variables | awk -F= '{print $2}' | sed -e 's/^"//' -e 's/"$//')

                if [[ ! -z $variables && ! -z $key && ! -z $value ]]; then

                    ## This will handle something like this url="https://fake.url.com"
                    # url="`grep -E '(https|http|ftp|file)://' <<< $value`"
                    url=$(grep -E '(https|http|ftp|file)://' <<< $value)
                    if [[ ( ! -z $url ) ]]; then
                        detect_unknown_urls "$line" '1'
                    fi
                fi

                ## This will handle something like this curl https://fake.url.com
                # url="`grep -E '(https|http|ftp|file)://' <<< $line`"
                url=$(grep -E '(https|http|ftp|file)://' <<< $line)
                if [[ ( "$line" =~ "curl" || "$line" =~ "wget" ) && ( ! -z $url ) ]]; then
                    detect_unknown_urls "$line" '2'
                fi
                
            fi
        done < $file
    fi
#  done
