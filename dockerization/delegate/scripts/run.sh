#!/bin/bash

bash ./modify_scripts.sh
bash ./start.sh && while [[ ! -e delegate.log ]]; do sleep 10s; done && tail -f delegate.log
