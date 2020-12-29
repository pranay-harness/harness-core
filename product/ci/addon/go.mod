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
	github.com/stretchr/testify v1.6.1
	github.com/vdemeester/k8s-pkg-credentialprovider v1.17.4
	github.com/wings-software/portal/commons/go/lib v0.0.0-00010101000000-000000000000
	go.uber.org/zap v1.16.0
	golang.org/x/net v0.0.0-20201031054903-ff519b6c9102
	google.golang.org/grpc v1.32.0
	k8s.io/api v0.17.4
	mvdan.cc/sh v2.6.4+incompatible
	mvdan.cc/sh/v3 v3.2.1
)