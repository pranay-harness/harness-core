#!/bin/bash

source set_default_variables.sh
bash ./replace_configs.sh
bash ./replace_hazelcast.sh
bash ./start_process.sh