module github.com/harness/harness-core/commons/go/lib

go 1.14

require (
	cloud.google.com/go/secretmanager v1.9.0
	cloud.google.com/go/storage v1.27.0
	github.com/aws/aws-sdk-go v1.34.29
	github.com/blendle/zapdriver v1.3.1
	github.com/cenkalti/backoff/v4 v4.1.0
	github.com/go-sql-driver/mysql v1.5.0
	github.com/gofrs/uuid v4.2.0+incompatible
	github.com/golang/groupcache v0.0.0-20210331224755-41bb18bfe9da // indirect
	github.com/golang/mock v1.6.0
	github.com/google/go-containerregistry v0.3.0
	github.com/grpc-ecosystem/go-grpc-middleware v1.2.1
	github.com/hashicorp/go-multierror v1.1.0
	github.com/lib/pq v1.10.9
	github.com/mattn/go-zglob v0.0.4
	github.com/minio/minio-go/v6 v6.0.57
	github.com/opentracing/opentracing-go v1.2.0
	github.com/pkg/errors v0.9.1
	github.com/satori/go.uuid v1.2.0
	github.com/shirou/gopsutil/v3 v3.21.1
	github.com/sirupsen/logrus v1.8.1
	github.com/smartystreets/goconvey v1.6.4 // indirect
	github.com/stretchr/testify v1.8.1
	github.com/vdemeester/k8s-pkg-credentialprovider v1.18.1-0.20201019120933-f1d16962a4db
	go.uber.org/zap v1.15.0
	google.golang.org/api v0.103.0
	google.golang.org/genproto v0.0.0-20230110181048-76db0878b65f
	google.golang.org/grpc v1.53.0
	gopkg.in/DATA-DOG/go-sqlmock.v1 v1.3.0
	k8s.io/api v0.20.1
	mvdan.cc/sh/v3 v3.7.0
	sigs.k8s.io/structured-merge-diff v1.0.1-0.20191108220359-b1b620dd3f06 // indirect
)
