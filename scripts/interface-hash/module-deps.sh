#!/usr/bin/env bash

MODULE_NAME="//800-pipeline-service:module"
declare -a LIST_OF_PROTO_FILES

get_proto_files_of_module() {
	LIST_OF_PROTO_FILES=`bazel query "deps($MODULE_NAME)" | grep -i "\.proto" | grep -i -v "com_google_protobuf"`
}

for PROTO_FILE in `bazel query "deps($MODULE_NAME)" | grep -i "\.proto" | grep -i -v "com_google_protobuf"`
	do
		PROTO_FILE_2=`echo ${PROTO_FILE//\/\//}`
		PROTO_FILE_3=`echo ${PROTO_FILE_2//:/\/}`
    	PACKAGE_NAME=`cat $PROTO_FILE_3 | grep -i package | grep -i -v go_package | cut -f 2 -d " " | cut -f 1 -d ";"`
    	for MESSAGE_NAME in `cat $PROTO_FILE_3 | grep -i "message" | grep -i "{" | cut -f 2 -d " "`
    		do
    			echo "$PACKAGE_NAME.$MESSAGE_NAME"
    		done
    	for ENUM_NAME in `cat $PROTO_FILE_3 | grep -i "enum" | grep -i "{" | cut -f 2 -d " "`
    		do
    			echo "$PACKAGE_NAME.$ENUM_NAME"
    		done
    done