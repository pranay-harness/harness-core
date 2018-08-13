#!/bin/bash

bash ./modify_scripts.sh
bash ./start.sh && while [[ $(ps -ef|grep java|wc -l) -gt 1 ]]; do sleep 10s; done
