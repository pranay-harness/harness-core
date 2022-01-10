# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
