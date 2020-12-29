package logutil

import (
	"github.com/wings-software/portal/commons/go/lib/logs"
	logger "github.com/wings-software/portal/product/ci/logger/util"
)

var (
	getLogKey = logger.GetLogKey
)

// GetGrpcRemoteLogger is a helper method that returns a logger than can communicate with the
// gRPC log server hosted on lite engine.
func GetGrpcRemoteLogger(stepID string) (*logs.RemoteLogger, error) {
	key, err := getLogKey(stepID)
	if err != nil {
		return nil, err
	}
	grpcClient, err := NewGrpcRemoteClient()
	if err != nil {
		return nil, err
	}
	writer, err := logs.NewRemoteWriter(grpcClient, key)
	if err != nil {
		return nil, err
	}
	rl, err := logs.NewRemoteLogger(writer)
	if err != nil {
		return nil, err
	}
	return rl, nil
}
