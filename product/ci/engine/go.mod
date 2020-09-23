module github.com/wings-software/portal/product/ci/engine

go 1.14

replace github.com/wings-software/portal/commons/go/lib => ../../../commons/go/lib
replace github.com/wings-software/portal/product/log-service => ../../../product/log-service

require (
	github.com/alexflint/go-arg v1.3.0
	github.com/cenkalti/backoff/v4 v4.0.2
	github.com/cespare/xxhash v1.1.0
	github.com/gofrs/uuid v3.3.0+incompatible
	github.com/golang/mock v1.4.3
	github.com/golang/protobuf v1.4.1
	github.com/grpc-ecosystem/go-grpc-middleware v1.2.1
	github.com/minio/minio-go/v6 v6.0.57
	github.com/pkg/errors v0.9.1
	github.com/stretchr/testify v1.5.1
	github.com/wings-software/portal/commons/go/lib v0.0.0-00010101000000-000000000000
	go.uber.org/zap v1.15.0
	golang.org/x/net v0.0.0-20200506145744-7e3656a0809f
	google.golang.org/grpc v1.31.0
)
