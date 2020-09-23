# Log Service for Harness

This branch provides a sample log storage and streming service using code extracted from Drone. The service uses an embedded bolt database for blob storage and an embedded in-memory stream. The blog storage engine can be replaced with s3 or any s3 compatible storage server.

This repository also includes a Go client for integrating with the server, and command line tools that can be used during local development for testing purposes.

# Server

Start the log server

```
$ log-service server
```

# Storage

Upload a log file to the log server

```
$ log-server cistore upload <projectID> <buildID> <stageID> <stepID> [<path>]
$ log-server cistore upload 1 2 3 4 /path/to/logs.json
```

Download a log file from the log server

```
$ log-server cistore download <projectID> <buildID> <stageID> <stepID>
$ log-server cistore download 1 2 3 4
```

Create a download link (S3 only)

```
$ log-server cistore download-link <projectID> <buildID> <stageID> <stepID>
$ log-server cistore download-link 1 2 3 4
```

Create an upload link (S3 only)

```
$ log-server cistore upload-link <projectID> <buildID> <stageID> <stepID>
$ log-server cistore upload-link 1 2 3 4
```

_Note that upload and download links can be a secure, scalable alternative to directly uploading files through the service. Also note that we prefer REST for uploading and downloading blobs. GRPC is less efficient when it comes to transferring blobs due to message size limits and having to serialize / deserialize contents._

# Streaming

Create a log stream:

```
$ log-server cistream open <projectID> <buildID> <stageID> <stepID>
```

Publish a log entry to the log stream:

```
$ log-server cistream push [<flags>] <projectID> <buildID> <stageID> <stepID> [<message>]
$ log-server cistream push 1 2 3 4 "echo hello"
$ log-server cistream push 1 2 3 4 "echo world"
```

Subscribe to a log stream:

```
$ log-server cistream tail <projectID> <buildID> <stageID> <stepID>
$ log-server cistream tail 1 2 3 4
```

Close the log stream:

```
$ log-server cistream close <projectID> <buildID> <stageID> <stepID>
$ log-server cistream close 1 2 3 4
```

Gather information about all active log streams, including the size of the stream and the number of connected subscribers:

```
$ log-server cistream info
```

# Adding API endpoints
To add your own API endpoints for interacting with the log service, add them in handler/service similar to handler/ci and update handler/handler.go to include the new routes.
Optionally, add a client for them under client/service to enable interaction/testing with the CLI in case a Golang client is needed.

# S3 Backend

The S3 backend can be enabled and configured by setting the following environment variables. _Note that the service can read environment variables from an .env file in the working directory_.

```text
LOG_SERVICE_MINIO_BUCKET=logs
LOG_SERVICE_MINIO_PREFIX=
LOG_SERVICE_MINIO_ENDPOINT=http://localhost:9000
LOG_SERVICE_MINIO_PATH_STYLE=true

AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS_REGION=us-east-1
```

_You can test the Minio engine locally by running Minio in Docker. Be sure to login to Minio and create the default bucket.

```text
docker run -p 9000:9000 \
  --name minio1 \
  -v /tmp/minio:/data \
  -e "MINIO_ACCESS_KEY=AKIAIOSFODNN7EXAMPLE" \
  -e "MINIO_SECRET_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY" \
  minio/minio server /data
```

# TODO

- Implement authentication mechanism
- Improve the ring buffer efficiency
- Improve the stream push endpoint to accept multiple log entries
- Implement a more scalable streaming backend (redis, etc)
- Implement a grpc client and server for the stream service only
