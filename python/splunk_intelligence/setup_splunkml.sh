#! /bin/bash
sudo easy_install virtualenv
make init
make dist
export SPLUNKML_ROOT=`pwd`/dist
echo $SPLUNKML_ROOT
