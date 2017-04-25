#kill delegate
kill -9 `cat `

#kill manager
kill -9 `cat rest/manager.pid`

#take dump of mongodb
mongodump

#tar.gz dump files
tar -czvf dump.tar.gz dump/
