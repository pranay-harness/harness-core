module github.com/wings-software/portal/product/ci/engine

go 1.14

replace github.com/wings-software/portal/commons/go/lib => ../../../commons/go/lib

require (
	github.com/alexflint/go-arg v1.3.0
	github.com/golang/mock v1.4.3
	github.com/golang/protobuf v1.4.1
	github.com/stretchr/testify v1.5.1
	github.com/wings-software/portal/commons/go/lib v0.0.0-00010101000000-000000000000
	go.uber.org/zap v1.15.0
)
