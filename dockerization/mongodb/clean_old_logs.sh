# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0


cleanLogs() {
  while true;
  do
    find /var/log/mongodb-mms-automation -mtime +2 -type f -delete
    sleep 30m
  done
}

cleanLogs &
supervisord -c ${MMS_HOME}/files/supervisor.conf
