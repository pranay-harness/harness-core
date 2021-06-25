# Log Service for Harness

Harness Log service. Used for live streaming logs as well as object store uploads.

# Generating the binary

Mac:
```
$ bazel build //product/log-service/...
```

Linux-based:
```
$ bazel build //product/log-service/...
$ bazel build --platforms=@io_bazel_rules_go//go/toolchain:linux_amd64 //product/log-service/...
```

# Accessing the binary

Mac:
```
$ $(bazel info bazel-bin)/product/log-service/darwin_amd64_stripped/log-service
```

Linux-based:
```
$ $(bazel info bazel-bin)/product/log-service/linux_amd64_pure_stripped/log-service
```

# Env variables for auth

If you're running log service locally, chances are you don't want to worry about auth. In that case, set
```
$ export LOG_SERVICE_DISABLE_AUTH=true
```

If you are enthusiastic and want to try out auth, please add the below variables. These are used for token generation
and authentication with the log server.
```
$ export LOG_SERVICE_GLOBAL_TOKEN=token
$ export LOG_SERVICE_SECRET=secret
```

# Other env variables

## Streaming:
If no env variables are specified, the log service will use an in memory stream.
To use redis instead, install redis locally and set the following env variables to talk to it
```
export LOG_SERVICE_REDIS_ENDPOINT=localhost:6379
export LOG_SERVICE_REDIS_PASSWORD=***
```

## Store:
If no env variables are specified, the log service will use an in memory store (bolt DB).
To use S3 instead, set up the following env variables:
```
export LOG_SERVICE_S3_BUCKET=logs
export LOG_SERVICE_S3_PREFIX=
export LOG_SERVICE_S3_ENDPOINT=http://localhost:9000
export LOG_SERVICE_S3_PATH_STYLE=true
export LOG_SERVICE_S3_ACCESS_KEY_ID=***
export LOG_SERVICE_S3_SECRET_ACCESS_KEY=***
export LOG_SERVICE_S3_REGION=us-east-1
```
The above is compatible with GCS as well (you can setup access and secret keys for a service account in the settings and use those)

Run minio locally using:
```
docker run -p 9000:9000 \
  --name minio1 \
  -v /tmp/minio:/data \
  -e "MINIO_ACCESS_KEY=AKIAIOSFODNN7EXAMPLE" \
  -e "MINIO_SECRET_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY" \
  minio/minio server /data
```

# Using the CLI

Get all options for the CLI:
```
$ <path-to-binary> help
``` 

# Server

Start the log server

```
$ <path-to-binary> server
```

# Token Generation

Replace the token and the endpoint with whatever values you’re using. Should be the same if you’re following the steps here. The token received is a per account token so all calls to the service need to use that token and the same account ID specified as above. This token will expire every 48 hours.

```
curl  -H "X-Harness-Token: token" -v GET http://localhost:8079/token\?accountID\=blah
```

# Storage

## Write to store

There are two ways to do this:

i) Generate an upload link and use that to upload (only for S3)
```
curl  -H "X-Harness-Token: <token-received-above>" -v POST http://localhost:8079/blob/link/upload?accountID\=blah\&key=sample
```

ii) Use the log service directly to upload
```
curl  -H "X-Harness-Token: <token-received-above>" -v POST http://localhost:8079/blob?accountID\=blah\&key=sample (with the request body)
```

_Note that upload and download links can be a secure, scalable alternative to directly uploading files through the service. Also note that we prefer REST for uploading and downloading blobs. GRPC is less efficient when it comes to transferring blobs due to message size limits and having to serialize / deserialize contents._

# Streaming

## Open a log stream:

```
$ curl -H "X-Harness-Token: <account-token>" -v -X POST http://localhost:8079/stream\?accountID\=blah\&key\=sample
```

## Write to stream:

Run same command as opening a log stream but with the above request body and use PUT
```
Request body example: 
[
{"level":"INFO","pos":1,"out":"Will be cloning..\n","time":"2020-09-23T12:38:18.308525+05:30","args":{}},
{"level":"INFO","pos":2,"out":"Cloning into 'small-test-repo'...\n","time":"2020-09-23T12:38:28.327238+05:30","args":{}},
]
```

## Tail a stream:

Format is server-sent events https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events
```
$ curl  -H "X-Harness-Token: <account-token>" -v GET http://localhost:8079/stream\?accountID\=blah\&key=sample

```

Close a stream:

```
curl -H "X-Harness-Token: <account-token>" -v -X DELETE http://localhost:8079/stream\?accountID\=blah\&key\=sample
```

# TODO

- Improve the ring buffer efficiency
- Implement a grpc client and server for the stream service only
