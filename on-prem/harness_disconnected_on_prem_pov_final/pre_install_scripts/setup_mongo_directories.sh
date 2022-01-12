#!/bin/bash
# Copyright 2019 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

echo "Executing commands to setup correct directories and its permissions"

if [[ -z $1 ]]; then
   runtime_dir=$HOME/harness_runtime
   echo "No runtime directory supplied in argument, will default to the home directory, value=$runtime_dir"
else
  runtime_dir=$1
  echo "Using runtime directory $runtime_dir"
fi

function getProperty () {
   FILENAME=$1
   PROP_KEY=$2
   PROP_VALUE=`cat "$FILENAME" | grep "$PROP_KEY" | cut -d'=' -f2`
   echo $PROP_VALUE
}


echo "Deleting existing mongo config files in dir ${runtime_dir}/config/mongo"


rm -rf ${runtime_dir}/config/mongo


mkdir -p ${runtime_dir}/config


echo "Copying mongo config to dir ${runtime_dir}/config/mongo"


cp -Rf ../config_template/mongo ${runtime_dir}/config/


echo "##### Setting up directories and appropriate permissions for mongodb######## "

chmod 666 ${runtime_dir}/config/mongo/mongod.conf
chmod 666 ${runtime_dir}/config/mongo/add_first_user.js
chmod 666 ${runtime_dir}/config/mongo/add_learning_engine_secret.js
chmod 666 ${runtime_dir}/config/mongo/publish_version.js

mongodb_data_dir=$(getProperty "../config_template/mongo/mongoconfig.properties" "mongodb_data_dir")
mongodb_sys_log_file=$(getProperty "../config_template/mongo/mongoconfig.properties" "mongodb_sys_log_file")

mkdir -p $runtime_dir/mongo/$mongodb_sys_log_dir
mkdir -p $runtime_dir/mongo/$mongodb_data_dir
touch $runtime_dir/mongo/$mongodb_sys_log_dir/$mongodb_sys_log_file

echo "Creating file : $runtime_dir/mongo/$mongodb_sys_log_dir/$mongodb_sys_log_file"

mv ${runtime_dir}/config/mongo/mongod.conf $runtime_dir/mongo
mkdir -p $runtime_dir/mongo/scripts
mv ${runtime_dir}/config/mongo/add_first_user.js $runtime_dir/mongo/scripts
mv ${runtime_dir}/config/mongo/add_learning_engine_secret.js $runtime_dir/mongo/scripts
mv ${runtime_dir}/config/mongo/publish_version.js $runtime_dir/mongo/scripts

chmod 777 -R ${runtime_dir}
chown -R 999 $runtime_dir/mongo/*
chmod 777 $runtime_dir/mongo/$mongodb_data_dir

echo "##### Created and updated appropriate permissions for mongodb files and directories ######## "
