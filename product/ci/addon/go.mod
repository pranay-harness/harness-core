module github.com/wings-software/portal/product/ci/addon

go 1.14

replace github.com/wings-software/portal/commons/go/lib => ../../../commons/go/lib

replace github.com/wings-software/portal/product/log-service => ../../../product/log-service

require (
	github.com/alexflint/go-arg v1.3.0
	github.com/golang/mock v1.4.4
	github.com/google/go-containerregistry v0.1.2
	github.com/grpc-ecosystem/go-grpc-middleware v1.2.1
	github.com/pkg/errors v0.9.1
	github.com/stretchr/testify v1.5.1
	github.com/wings-software/portal/commons/go/lib v0.0.0-00010101000000-000000000000
	go.uber.org/zap v1.16.0
	golang.org/x/net v0.0.0-20200904194848-62affa334b73
	google.golang.org/grpc v1.32.0
)
