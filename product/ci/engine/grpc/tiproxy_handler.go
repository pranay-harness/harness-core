package grpc

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/pkg/errors"
	fs "github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/product/ci/addon/ti"
	"github.com/wings-software/portal/product/ci/common/avro"
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

const (
	cgSchemaPath = "callgraph.avsc"
)

// handler is used to implement EngineServer
type tiProxyHandler struct {
	log *zap.SugaredLogger
}

// NewTiProxyHandler returns a GRPC handler that implements pb.TiProxyServer
func NewTiProxyHandler(log *zap.SugaredLogger) pb.TiProxyServer {
	return &tiProxyHandler{log}
}

// SelectTests gets the list of selected tests to be run.
// TODO: Stream the response as there is a 4MB limit on message sizes in gRPC
func (h *tiProxyHandler) SelectTests(ctx context.Context, req *pb.SelectTestsRequest) (*pb.SelectTestsResponse, error) {
	var err error
	tc, err := remoteTiClient()
	if err != nil {
		h.log.Errorw("could not create a client to the TI service", zap.Error(err))
		return nil, err
	}
	step := req.GetStepId()
	if step == "" {
		return nil, errors.New("step ID not present in request")
	}
	sha := req.GetSha()
	if sha == "" {
		return nil, errors.New("commit ID not present in request")
	}
	repo := req.GetRepo()
	if repo == "" {
		return nil, errors.New("repo not present in request")
	}
	branch := req.GetBranch()
	if branch == "" {
		return nil, errors.New("branch not present in request")
	}
	diffFiles := req.GetDiffFiles()
	org, err := getOrgId()
	if err != nil {
		return nil, err
	}
	project, err := getProjectId()
	if err != nil {
		return nil, err
	}
	pipeline, err := getPipelineId()
	if err != nil {
		return nil, err
	}
	build, err := getBuildId()
	if err != nil {
		return nil, err
	}
	stage, err := getStageId()
	if err != nil {
		return nil, err
	}
	tests, err := tc.SelectTests(org, project, pipeline, build, stage, step, repo, sha, branch, diffFiles)
	if err != nil {
		return nil, err
	}

	jsonTests := []string{}
	for _, t := range tests {
		jsonBytes, err := json.Marshal(t)
		if err != nil {
			return nil, err
		}
		jsonTests = append(jsonTests, string(jsonBytes))
	}
	return &pb.SelectTestsResponse{
		Tests: jsonTests,
	}, nil
}

// WriteTests writes tests to the TI service.
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

func (h *tiProxyHandler) UploadCg(ctx context.Context, req *pb.UploadCgRequest) (*pb.UploadCgResponse, error) {
	step := req.GetStepId()
	res := &pb.UploadCgResponse{}
	if step == "" {
		return res, fmt.Errorf("step ID not present in request")
		return res, nil
	}
	sha := req.GetSha()
	if sha == "" {
		return res, fmt.Errorf("commit ID not present in request")
	}
	repo := req.GetRepo()
	if repo == "" {
		return res, fmt.Errorf("repo not present in request")
	}
	branch := req.GetBranch()
	if branch == "" {
		return res, fmt.Errorf("branch not present in request")
	}

	cgDir := "/Users/amansingh/Desktop/f2.txt"
	fs := fs.NewOSFileSystem(h.log)
	parser := ti.NewCallGraphParser(h.log, fs)
	cg, err := parser.Parse(cgDir)
	if err != nil {
		return res, errors.Wrap(err, "failed to parse callgraph directory")
	}
	cgMap := cg.ToStringMap()
	cgSer, err := avro.NewCgphSerialzer(cgSchemaPath)
	if err != nil {
		return res, errors.Wrap(err, "failed to create serializer")
	}
	encBytes, err := cgSer.Serialize(cgMap)
	if err != nil {
		return res, errors.Wrap(err, "failed to encode callgraph")
	}
	client, err := remoteTiClient()
	if err != nil {
		return res, errors.Wrap(err, "failed to create tiClient")
	}
	org, err := getOrgId()
	if err != nil {
		return res, errors.Wrap(err, "org id not found")
	}
	project, err := getProjectId()
	if err != nil {
		return res, errors.Wrap(err, "project id not found")
	}
	pipeline, err := getPipelineId()
	if err != nil {
		return res, errors.Wrap(err, "pipeline id not found")
	}
	build, err := getBuildId()
	if err != nil {
		return res, errors.Wrap(err, "build id not found")
	}
	stage, err := getStageId()
	if err != nil {
		return res, errors.Wrap(err, "stage id not found")
	}
	err = client.UploadCg(org, project, pipeline, build, stage, step, repo, sha, branch, encBytes)
	if err != nil {
		return res, errors.Wrap(err, "failed to upload cg to ti server")
	}
	return res, nil
}
