#!/usr/bin/env bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
