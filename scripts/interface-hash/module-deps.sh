#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


for PROTO_FILE in `bazel query "deps($1)" | grep -i "\.proto" | grep -i -v "com_google_protobuf"`
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
