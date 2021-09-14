# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

## If you want to start from migration version `x` (for saas if the migrations are already done manually),
## use SET_MONGO_MIGRATION_VERSION and SET_TSDB_MIGRATION_VERSION variables for setting the starting version of migrations

## Sample command for force set version:
## ./migrate -path <path> -database <db url> force 4
## Sample command for migration:
## ./migrate -path <path> -database <db url> up

if [[ "" != "$SET_MONGO_MIGRATION_VERSION" ]]; then
  echo "Setting mongo db version to: "$SET_MONGO_MIGRATION_VERSION
  ./migrate -path migrations/mongodb -database $MONGO_DB_URL force $SET_MONGO_MIGRATION_VERSION
fi
echo "migrating mongo db to latest version"
./migrate -path migrations/mongodb -database $MONGO_DB_URL up

if [[ "" != "$SET_TSDB_MIGRATION_VERSION" ]]; then
  echo "Setting tsdb version to: "$SET_TSDB_MIGRATION_VERSION
  ./migrate -path migrations/tsdb -database $TSDB_URL force $SET_TSDB_MIGRATION_VERSION
fi

echo "migrating tsdb to latest version"
./migrate -path migrations/tsdb -database $TSDB_URL up

echo "Finished TI Service migrations"
