# Test Intelligence Service for Harness CI

Harness TI service. Does all things tests.

# Generating the binary

Mac:
```
$ bazel build //product/...
```

Linux-based:
```
$ bazel build //product/...
$ bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //product/...
```

# Accessing the binary

Mac:
```
$ $(bazel info bazel-bin)/product/ci/ti-service/darwin_amd64_stripped/ti-service
```

Linux-based:
```
$ $(bazel info bazel-bin)/product/ci/ti-service/linux_amd64_pure_stripped/ti-service
```

# Server

Start the TI server

```
$ <path-to-binary> server
```

# Database

TI service uses timescaleDB as a backend.

Environment variables needed:
```
export TI_SERVICE_TIMESCALE_USERNAME=postgres
export TI_SERVICE_TIMESCALE_PASSWORD=
export TI_SERVICE_TIMESCALE_HOST=localhost
export TI_SERVICE_TIMESCALE_PORT=5432
export TI_SERVICE_DB_NAME=postgres
export TI_SERVICE_HYPER_TABLE=tests
```

Installation of timescaleDB and postgres is needed.

# Setting up TimescaleDB locally

Log into Postgres:
```
psql -U postgres -h localhost 
```

Install the timescale extension:
```
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;
```

Create the tests table:
```
CREATE TABLE tests(
  time        TIMESTAMPTZ       NOT NULL,
  account_id TEXT NOT NULL,
  org_id TEXT NOT NULL,
  project_id TEXT NOT NULL,
  pipeline_id TEXT NOT NULL,
  build_id TEXT NOT NULL,
  stage_id TEXT NOT NULL,
  step_id TEXT NOT NULL,
  report TEXT NOT NULL,
  name TEXT NOT NULL,
  suite_name TEXT NOT NULL,
  class_name TEXT,
  duration_ms INT,
  status TEXT,
  message TEXT,
  description TEXT,
  type TEXT,
  stdout TEXT,
  stderr TEXT
);
SELECT create_distributed_hypertable('tests', 'time');
```

# Rules for migrations and running migrations on environments:

## Issues with using migrate tool as opposed to creating tickets for migrations:

i) Can’t move to a 0 version in the tool.
ii) A failed migration will mark the DB state as dirty and the version will need to be forced after that. This means that during deployment, if migration fails - we will have to perform this operation.

## How to run migrations on prod:

i) As long as migrations are successful, version will get updated after each deployment and we will continue from there.
ii) In case migration X fails on prod, we need to force the migration to (X-1), fix the migration and redeploy.

## Migration rules:

i) Always add indexes / schema updates in the migrations files whenever any code change is done in a PR.
ii) We will use these migrations for all environments. Make sure it is tested properly on QA and locally. Local test would be to create a new collection (in mongo) or to use a new table in timescale and run all the migrations. It should succeed.
                 Example: migrate -source file://product/ci/ti-service/migrations/mongodb -database mongodb://localhost/anyNewDB --verbose up
ii) All mongo index creations should have keys in alphabetical order followed by 1.0. Eg:
	keys: {
		“a”: 1.0,
		“b”: 1.0,
		“c”: 1.0
	}
The names should be underscore separated on the keys. Key for the above example would look like a_1_b_1_c_1. This will prevent any future clashes of indexes and if so, should be caught locally.
iii) Timescale migrations which add rows should also add in any new indexes needed. Each row should have a comment as to why it’s needed.
iv) Always run JSON lint on mongo migration files so that they are easy to read.



