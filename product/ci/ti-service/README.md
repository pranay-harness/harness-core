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
SELECT create_hypertable('tests', 'time');
```