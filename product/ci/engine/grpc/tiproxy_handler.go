package grpc

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/pkg/errors"
	fs "github.com/wings-software/portal/commons/go/lib/filesystem"
	visp "github.com/wings-software/portal/product/ci/addon/parser/visualization"
	"github.com/wings-software/portal/product/ci/common/avro"
	"io"
	"path/filepath"
	"strings"

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
	cgSchemaType       = "callgraph"
	visGraphSchematype = "visgraph"
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
	sha := req.GetSha() // may not be set for manual execution
	repo := req.GetRepo()
	if repo == "" {
		return nil, errors.New("repo not present in request")
	}
	source := req.GetSourceBranch() // may not be set for manual execution
	target := req.GetTargetBranch()
	if target == "" {
		return nil, errors.New("target branch not present in request")
	}
	body := req.GetBody()
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
	selection, err := tc.SelectTests(org, project, pipeline, build, stage, step, repo, sha, source, target, body)
	if err != nil {
		return nil, err
	}

	jsonStr, err := json.Marshal(selection)
	if err != nil {
		return &pb.SelectTestsResponse{}, err
	}
	return &pb.SelectTestsResponse{
		Selected: string(jsonStr),
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
	repo := ""
	sha := ""
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
		repo = msg.GetRepo()
		sha = msg.GetSha()
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
	err = tc.Write(stream.Context(), org, project, pipeline, build, stage, stepId, report, repo, sha, tests)
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
	}
	sha := req.GetSha()
	repo := req.GetRepo()
	if repo == "" {
		return res, fmt.Errorf("repo not present in request")
	}
	source := req.GetSource()
	if source == "" {
		return res, fmt.Errorf("source branch not present in request")
	}
	target := req.GetTarget()
	if target == "" {
		return res, fmt.Errorf("target branch not present in request")
	}
	timeMs := req.GetTimeMs()
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

	//Upload callgraph to TI server
	encCg, err := h.getEncodedData("json", cgSchemaType, req)
	if err != nil {
		return res, errors.Wrap(err, "failed to get avro encoded callgraph")
	}
	err = client.UploadCg(org, project, pipeline, build, stage, step, repo, sha, source, target, timeMs, encCg)
	if err != nil {
		return res, errors.Wrap(err, "failed to upload cg to ti server")
	}

	// Upload visGraph to ti server
	encVg, err := h.getEncodedData("csv", visGraphSchematype, req)
	if err != nil {
		return res, errors.Wrap(err, "failed to get avro encoded visGraph")
	}
	// Upload callgraph to TI server
	err = client.UploadVisgraph(org, project, pipeline, build, stage, step, repo, sha, source, target, encVg)
	if err != nil {
		return res, errors.Wrap(err, "failed to upload visgraph to ti server")
	}
	return res, nil
}

// getCgFiles return list of cg files in given directory
func (h *tiProxyHandler) getCgFiles(dir, ext string) ([]string, error) {
	if !strings.HasSuffix(dir, "/") {
		dir = dir + "/"
	}
	return filepath.Glob(dir + "*." + ext)
}

// getEncodedData reads all files of specified format from datadir folder and returns byte array of avro encoded format
func (h *tiProxyHandler) getEncodedData(fExtension, schemaType string, req *pb.UploadCgRequest) ([]byte, error) {
	var parser visp.Parser

	visDir := req.GetDataDir()
	if visDir == "" {
		return nil, fmt.Errorf("dataDir not present in request")
	}
	files, err := h.getCgFiles(visDir, fExtension)
	if err != nil {
		return nil, errors.Wrap(err, "failed to fetch files inside the directory")
	}
	fs := fs.NewOSFileSystem(h.log)
	parser = visp.NewVisGraphParser(h.log, fs)
	visGraph, err := parser.Parse(files)
	if err != nil {
		return nil, errors.Wrap(err, "failed to parse visgraph")
	}
	h.log.Infow(fmt.Sprintf("size of nodes: %d", len(visGraph.Keys)))

	cgMap := visGraph.ToStringMap()
	cgSer, err := avro.NewCgphSerialzer(schemaType)
	if err != nil {
		return nil, errors.Wrap(err, "failed to create serializer")
	}
	encCg, err := cgSer.Serialize(cgMap)
	if err != nil {
		return nil, errors.Wrap(err, "failed to encode visgraph")
	}
	return encCg, nil
}
