package logutil

import (
	"github.com/wings-software/portal/commons/go/lib/logs"
)

// GetGrpcRemoteLogger is a helper method that returns a logger than can communicate with the
// gRPC log server hosted on lite engine.
func GetGrpcRemoteLogger(key string) (*logs.RemoteLogger, error) {
	grpcClient, err := NewGrpcRemoteClient()
	if err != nil {
		return nil, err
	}
	rw, err := logs.NewRemoteWriter(grpcClient, key)
	if err != nil {
		return nil, err
	}
	rws := logs.NewReplacer(rw, external.GetSecrets()) // Remote writer with secrets masked
	rl, err := logs.NewRemoteLogger(rws)
	if err != nil {
		return nil, err
	}
	return rl, nil
}
