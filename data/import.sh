# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

#mongoimport --db test_ti --collection nodes \
#       --authenticationDatabase admin --username admin --password adminpass \
#       --host 34.82.201.40 --port 27017 \
#       --drop --file ~/git/portal/data/nodes.json
#
#
#mongoimport --db test_ti --collection relations \
#       --authenticationDatabase admin --username admin --password adminpass \
#       --host 34.82.201.40 --port 27017 \
#       --drop --file ~/git/portal/data/relations.json



mongoimport --uri mongodb+srv://qa-i:ubNZGengapvcMx0U@qa-ci-0-0ioxn.mongodb.net/harness-ti \
--collection nodes \
--type json  --drop --file ~/git/portal/data/nodes.json


 mongoimport --uri mongodb+srv://qa-i:ubNZGengapvcMx0U@qa-ci-0-0ioxn.mongodb.net/harness-ti \
--collection relations \
--type json  --drop --file ~/git/portal/data/relations.json
