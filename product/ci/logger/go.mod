module github.com/wings-software/portal/product/ci/logger

replace github.com/wings-software/portal/commons/go/lib => ../../../commons/go/lib

replace github.com/wings-software/portal/product/log-service => ../../../product/log-service

go 1.14

require (
	github.com/alexflint/go-arg v1.3.0
	github.com/wings-software/portal/commons/go/lib v0.0.0-00010101000000-000000000000
	github.com/wings-software/portal/product/log-service v0.0.0-00010101000000-000000000000
	go.uber.org/zap v1.16.0
)
