module github.com/wings-software/portal/commons/go/lib

go 1.14

replace github.com/wings-software/portal/product/log-service => ../../../product/log-service

require (
	cloud.google.com/go/storage v1.8.0
	github.com/aws/aws-sdk-go v1.34.10
	github.com/cenkalti/backoff v2.2.1+incompatible // indirect
	github.com/cenkalti/backoff/v4 v4.0.2
	github.com/go-sql-driver/mysql v1.5.0
	github.com/golang/mock v1.4.4
	github.com/grpc-ecosystem/go-grpc-middleware v1.2.1
	github.com/hashicorp/go-multierror v1.1.0
	github.com/minio/minio-go/v6 v6.0.57
	github.com/opentracing/opentracing-go v1.1.0
	github.com/pkg/errors v0.9.1
	github.com/satori/go.uuid v1.2.0
	github.com/stretchr/testify v1.5.1
	github.com/wings-software/portal/product/log-service v0.0.0-00010101000000-000000000000
	go.uber.org/zap v1.15.0
	google.golang.org/api v0.24.0
	google.golang.org/grpc v1.29.1
	gopkg.in/DATA-DOG/go-sqlmock.v1 v1.3.0
)
