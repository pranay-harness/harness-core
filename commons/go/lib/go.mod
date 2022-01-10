module github.com/wings-software/portal/commons/go/lib

go 1.14

replace github.com/wings-software/portal/product/log-service => ../../../product/log-service

require (
	cloud.google.com/go/storage v1.8.0
	github.com/aws/aws-sdk-go v1.34.29
	github.com/blendle/zapdriver v1.3.1
	github.com/cenkalti/backoff v2.2.1+incompatible // indirect
	github.com/cenkalti/backoff/v4 v4.1.0
	github.com/go-sql-driver/mysql v1.5.0
	github.com/gofrs/uuid v4.2.0+incompatible
	github.com/golang/mock v1.4.4
	github.com/google/go-containerregistry v0.3.0
	github.com/grpc-ecosystem/go-grpc-middleware v1.2.1
	github.com/hashicorp/go-multierror v1.1.0
	github.com/hashicorp/golang-lru v0.5.4 // indirect
	github.com/hpcloud/tail v1.0.0
	github.com/minio/minio-go/v6 v6.0.57
	github.com/neelance/parallel v0.0.0-20160708114440-4de9ce63d14c // indirect
	github.com/opentracing/basictracer-go v1.1.0 // indirect
	github.com/opentracing/opentracing-go v1.2.0
	github.com/pkg/errors v0.9.1
	github.com/prometheus/client_golang v1.8.0 // indirect
	github.com/satori/go.uuid v1.2.0
	github.com/shirou/gopsutil/v3 v3.21.1
	github.com/slimsag/godocmd v0.0.0-20161025000126-a1005ad29fe3 // indirect
	github.com/sourcegraph/ctxvfs v0.0.0-20180418081416-2b65f1b1ea81 // indirect
	github.com/sourcegraph/go-langserver v2.0.0+incompatible // indirect
	github.com/sourcegraph/jsonrpc2 v0.0.0-20200429184054-15c2290dcb37 // indirect
	github.com/stretchr/testify v1.6.1
	github.com/vdemeester/k8s-pkg-credentialprovider v1.18.1-0.20201019120933-f1d16962a4db
	go.uber.org/zap v1.15.0
	golang.org/x/tools v0.0.0-20201105220310-78b158585360 // indirect
	google.golang.org/api v0.24.0
	google.golang.org/grpc v1.29.1
	google.golang.org/protobuf v1.25.0 // indirect
	gopkg.in/DATA-DOG/go-sqlmock.v1 v1.3.0
	gopkg.in/fsnotify.v1 v1.4.7 // indirect
	gopkg.in/tomb.v1 v1.0.0-20141024135613-dd632973f1e7 // indirect
	k8s.io/api v0.20.1
	sigs.k8s.io/structured-merge-diff v1.0.1-0.20191108220359-b1b620dd3f06 // indirect
)
