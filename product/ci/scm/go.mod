module github.com/wings-software/portal/product/ci/scm

go 1.14

replace github.com/wings-software/portal/commons/go/lib => ../../../commons/go/lib

require (
	github.com/alexflint/go-arg v1.3.0
	github.com/drone/go-scm v1.8.1-0.20210111143840-199eee180289
	github.com/drone/go-scm-codecommit v0.0.0-20210315104920-2d8b9dc5ed8a
	github.com/golang/mock v1.4.4
	github.com/golang/protobuf v1.4.3
	github.com/google/go-cmp v0.5.2
	github.com/grpc-ecosystem/go-grpc-middleware v1.2.1
	github.com/stretchr/testify v1.6.1
	github.com/wings-software/portal/commons/go/lib v0.0.0-00010101000000-000000000000
	github.com/wings-software/portal/product/log-service v0.0.0-20210305084455-298bbd5bd1fd // indirect
	go.uber.org/zap v1.15.0
	golang.org/x/net v0.0.0-20201110031124-69a78807bb2b
	google.golang.org/grpc v1.29.1
	mvdan.cc/sh/v3 v3.2.4 // indirect
)
