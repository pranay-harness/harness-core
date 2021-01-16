package grpc

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"

	"github.com/wings-software/portal/product/ci/common/external"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.uber.org/zap"
)

var (
	remoteTiClient = external.GetTiHTTPClient
	getOrgId       = external.GetOrgId
	getProjectId   = external.GetProjectId
	getPipelineId  = external.GetPipelineId
	getBuildId     = external.GetBuildId
	getStageId     = external.GetStageId
)

// handler is used to implement EngineServer
type tiProxyHandler struct {
	log *zap.SugaredLogger
}

// NewTiProxyHandler returns a GRPC handler that implements pb.TiProxyServer
func NewTiProxyHandler(log *zap.SugaredLogger) pb.TiProxyServer {
	return &tiProxyHandler{log}
}

// UploadUsingLink uploads using the link generated above.
func (h *tiProxyHandler) WriteTests(stream pb.TiProxy_WriteTestsServer) error {
	var err error
	tc, err := remoteTiClient()
	if err != nil {
		h.log.Errorw("could not create a client to the TI service", zap.Error(err))
		return err
	}
	var tests []*types.TestCase
	stepId := ""
	for {
		msg, err := stream.Recv()
		if err == io.EOF {
			break
		}
		if err != nil {
			h.log.Errorw("received error from client stream while trying to receive test case data to upload", zap.Error(err))
			continue
		}
		stepId = msg.GetStepId()
		ret := msg.GetTests()
		for _, bt := range ret {
			t := &types.TestCase{}
			err = json.Unmarshal([]byte(bt), t)
			if err != nil {
				return fmt.Errorf("could not unmarshal data: %s", err)
			}
			tests = append(tests, t)
		}
	}
	if stepId == "" {
		return errors.New("step ID not present in response")
	}
	org, err := getOrgId()
	if err != nil {
		return err
	}
	project, err := getProjectId()
	if err != nil {
		return err
	}
	pipeline, err := getPipelineId()
	if err != nil {
		return err
	}
	build, err := getBuildId()
	if err != nil {
		return err
	}
	stage, err := getStageId()
	if err != nil {
		return err
	}
	report := "junit" // get from proto if we need other reports in the future
	err = tc.Write(stream.Context(), org, project, pipeline, build, stage, stepId, report, tests)
	if err != nil {
		h.log.Errorw("could not write test cases: ", zap.Error(err))
		return err
	}
	err = stream.SendAndClose(&pb.WriteTestsResponse{})
	if err != nil {
		h.log.Errorw("could not close test case data protobuf stream", zap.Error(err))
		return err
	}
	h.log.Infow("parsed test cases", "num_cases", len(tests))
	return nil
}
