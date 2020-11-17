package grpc

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	logger "github.com/wings-software/portal/product/ci/logger/util"
	"github.com/wings-software/portal/product/log-service/stream"
	"go.uber.org/zap"
)

var (
	remoteLogClient = logger.GetRemoteHTTPClient
)

// handler is used to implement EngineServer
type logProxyHandler struct {
	log *zap.SugaredLogger
}

// NewEngineHandler returns a GRPC handler that implements pb.EngineServer
func NewLogProxyHandler(log *zap.SugaredLogger) pb.LogProxyServer {
	return &logProxyHandler{log}
}

// Write writes to a log stream.
// Connects to the log service to invoke the write to stream API.
func (h *logProxyHandler) Write(ctx context.Context, in *pb.WriteRequest) (*pb.WriteResponse, error) {
	lc, err := remoteLogClient()
	if err != nil {
		h.log.Errorw("Could not create a client to the log service", zap.Error(err))
		return &pb.WriteResponse{}, err
	}
	var lines []*stream.Line
	for _, strLine := range in.GetLines() {
		l := &stream.Line{}
		err = json.Unmarshal([]byte(strLine), l)
		if err != nil {
			h.log.Errorw(fmt.Sprintf("Unable to marshal received lines. First error instance: %s", strLine))
			return nil, fmt.Errorf("Could not unmarshal received lines. First error instance: %s", strLine)
		}
		lines = append(lines, l)
	}
	err = lc.Write(ctx, in.GetKey(), lines)
	if err != nil {
		h.log.Errorw("Could not write to the log stream", zap.Error(err))
		return &pb.WriteResponse{}, err
	}
	return &pb.WriteResponse{}, nil
}

// UploadLink returns an upload link for the logs.
// Connects to the log service to invoke the UploadLink to store API.
func (h *logProxyHandler) UploadLink(ctx context.Context, in *pb.UploadLinkRequest) (*pb.UploadLinkResponse, error) {
	lc, err := remoteLogClient()
	if err != nil {
		h.log.Errorw("Could not create a client to the log service", zap.Error(err))
		return &pb.UploadLinkResponse{}, err
	}
	link, err := lc.UploadLink(ctx, in.GetKey())
	if err != nil {
		h.log.Errorw("Could not generate an upload link for log upload", zap.Error(err))
		return &pb.UploadLinkResponse{}, err
	}
	return &pb.UploadLinkResponse{Link: link.Value}, nil
}

// UploadUsingLink uploads using the link generated above.
func (h *logProxyHandler) UploadUsingLink(ctx context.Context, in *pb.UploadUsingLinkRequest) (*pb.UploadUsingLinkResponse, error) {
	var err error
	lc, err := remoteLogClient()
	if err != nil {
		h.log.Errorw("Could not create a client to the log service", zap.Error(err))
		return &pb.UploadUsingLinkResponse{}, err
	}
	data := new(bytes.Buffer)
	for _, line := range in.GetLines() {
		data.Write([]byte(line))
	}
	err = lc.UploadUsingLink(ctx, in.GetLink(), data)
	if err != nil {
		h.log.Errorw("Could not upload logs using upload link", zap.Error(err))
		return &pb.UploadUsingLinkResponse{}, err
	}
	return &pb.UploadUsingLinkResponse{}, nil
}

// Open opens the log stream.
// Connects to the log service to invoke the open stream API.
func (h *logProxyHandler) Open(ctx context.Context, in *pb.OpenRequest) (*pb.OpenResponse, error) {
	var err error
	lc, err := remoteLogClient()
	if err != nil {
		h.log.Errorw("Could not create a client to the log service", zap.Error(err))
		return &pb.OpenResponse{}, err
	}
	err = lc.Open(ctx, in.GetKey())
	if err != nil {
		h.log.Errorw("Could not open log stream", zap.Error(err))
		return &pb.OpenResponse{}, err
	}
	return &pb.OpenResponse{}, nil
}

// Close closes the log stream.
// Connects to the log service and closes a stream.
func (h *logProxyHandler) Close(ctx context.Context, in *pb.CloseRequest) (*pb.CloseResponse, error) {
	var err error
	lc, err := remoteLogClient()
	if err != nil {
		h.log.Errorw("Could not create a client to the log service", zap.Error(err))
		return &pb.CloseResponse{}, err
	}
	err = lc.Close(ctx, in.GetKey())
	if err != nil {
		h.log.Errorw("Could not close log stream", zap.Error(err))
		return &pb.CloseResponse{}, err
	}
	return &pb.CloseResponse{}, nil
}
