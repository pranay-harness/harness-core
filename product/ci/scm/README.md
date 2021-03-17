# Using SCM

## Running in vscode / debug mode

1. This assumes you can run the following commands successfully from the root of `./portal`.

    ```BASH
    bazelisk build //product/ci/scm/proto/...
    bazelisk build //product/ci/scm/...
    gazelle
    ```

2. git change.

    ```BASH
    cat ~/.gitconfig
    [url "git@github.com:"]
      insteadOf = https://github.com/
    ```

3. go private EG `~/,zshrc`

    ```BASH
    export GOPRIVATE=github.com/wings-software/portal
    ```

4. Only open the scm folder in vscode `portal/product/ci/scm`, do not open from the portal root.
5. You will need to comment out the logservice line from `commons/go/lib/go.mod` Then you can run go build in `portal/product/ci/scm` or run the vscode debugger.
6. If you change the proto file you will need to run `bazelisk build //product/ci/scm/proto/...` to re-create the go file `scm.pb.go`. Then you can copy this file to `portal/product/ci/scm`.

    ```BASH
    cp /home/tp/.cache/bazel/_bazel_tp/529a9f5eb5d3c3de90f20271ededd500/execroot/harness_monorepo/bazel-out/k8-fastbuild/bin/product/ci/scm/proto/linux_amd64_stripped/ciscmpb_go_proto%/github.com/wings-software/portal/product/ci/scm/proto/scm.pb.go ~/workspace/portal/product/ci/scm/proto
    ```

7. If you change code dependencies you will need to re-run gazelle again to update the `BUILD.bazel` files in `portal/product/ci/scm`.

```BASH
diff --git a/commons/go/lib/go.mod b/commons/go/lib/go.mod
index bdfe753b01..2bef85919d 100644
--- a/commons/go/lib/go.mod
+++ b/commons/go/lib/go.mod
@@ -30,7 +30,7 @@ require (
        github.com/sourcegraph/jsonrpc2 v0.0.0-20200429184054-15c2290dcb37 // indirect
        github.com/stretchr/testify v1.6.1
        github.com/vdemeester/k8s-pkg-credentialprovider v1.18.1-0.20201019120933-f1d16962a4db
-       github.com/wings-software/portal/product/log-service v0.0.0-00010101000000-000000000000
+//     github.com/wings-software/portal/product/log-service v0.0.0-00010101000000-000000000000
        go.uber.org/zap v1.15.0
        golang.org/x/tools v0.0.0-20201105220310-78b158585360 // indirect
        google.golang.org/api v0.24.0
```

## Example requests

I tested using grpc calls using `https://github.com/uw-labs/bloomrpc`

### File requests

```BASH
// find
{
  "slug": "tphoney/scm-test",
  "path": "README.md",
  "branch": "main",
  "provider": {
    "github": {
      "access_token": "963408579168567c07ff8bfd2a5455e5307f74d4"
    }
  }
}
// create
{
  "slug": "tphoney/scm-test",
  "path": "newfile",
  "data": "data1",
  "message": "message1",
  "branch": "main",
  "signature": {
    "name": "tp honey",
    "email": "tp@harness.io"
  },
  "provider": {
    "github": {
      "access_token": "963408579168567c07ff8bfd2a5455e5307f74d4"
    }
  }
}
// find
{
  "slug": "tphoney/scm-test",
  "path": "newfile",
  "branch": "main",
  "provider": {
    "github": {
      "access_token": "963408579168567c07ff8bfd2a5455e5307f74d4"
    }
  }
}
// update
{
  "slug": "tphoney/scm-test",
  "path": "newfile",
  "data": "data2",
  "message": "message2",
  "branch": "main",
  "sha": "0abc8f194801d3d07af700bae67026ed2695ec59",
  "signature": {
    "name": "tp honey",
    "email": "tp@harness.io"
  },
  "provider": {
    "github": {
      "access_token": "963408579168567c07ff8bfd2a5455e5307f74d4"
    }
  }
}
// upsert - with create
{
  "slug": "tphoney/scm-test",
  "path": "upsert",
  "data": "data2",
  "message": "message2",
  "branch": "main",
  "signature": {
    "name": "tp honey",
    "email": "tp@harness.io"
  },
  "provider": {
    "github": {
      "access_token": "963408579168567c07ff8bfd2a5455e5307f74d4"
    }
  }
}
```
