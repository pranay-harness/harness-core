
cleanLogs() {
  while true;
  do
    find /var/log/mongodb-mms-automation -mtime +20 -type f -delete
    sleep 30m
  done
}

cleanLogs &
supervisord -c ${MMS_HOME}/files/supervisor.conf
