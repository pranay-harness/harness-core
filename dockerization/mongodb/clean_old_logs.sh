# Copyright 2019 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

cleanLogs() {
  while true;
  do
    find /var/log/mongodb-mms-automation -mtime +2 -type f -delete
    sleep 30m
  done
}

cleanLogs &
supervisord -c ${MMS_HOME}/files/supervisor.conf
