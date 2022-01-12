// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package tasks

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"os"

	"github.com/ghodss/yaml"
	grpc_retry "github.com/grpc-ecosystem/go-grpc-middleware/retry"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/product/ci/addon/testreports"
	"github.com/wings-software/portal/product/ci/addon/testreports/junit"
	"github.com/wings-software/portal/product/ci/common/external"
	"github.com/wings-software/portal/product/ci/engine/consts"
	grpcclient "github.com/wings-software/portal/product/ci/engine/grpc/client"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.uber.org/zap"
)

var (
	newJunit = junit.New
)

func collectCg(ctx context.Context, stepID, cgDir string, timeMs int64, log *zap.SugaredLogger) error {
	repo, err := external.GetRepo()
	if err != nil {
		return err
	}
	isManual := external.IsManualExecution()
	sha, err := external.GetSha()
	if err != nil && !isManual {
		return err
	}
	source, err := external.GetSourceBranch()
	if err != nil && !isManual {
		return err
	} else if isManual {
		source, err = external.GetBranch()
		if err != nil {
			return err
		}
	}
	target, err := external.GetTargetBranch()
	if err != nil && !isManual {
		return err
	} else if isManual {
		target, err = external.GetBranch()
		if err != nil {
			return err
		}
	}
	// Create TI proxy client (lite engine)
	client, err := grpcclient.NewTiProxyClient(consts.LiteEnginePort, log)
	if err != nil {
		return err
	}
	defer client.CloseConn()
	req := &pb.UploadCgRequest{
		StepId:  stepID,
		Repo:    repo,
		Sha:     sha,
		Source:  source,
		Target:  target,
		DataDir: cgDir,
		TimeMs:  timeMs,
	}
	log.Infow(fmt.Sprintf("sending cgRequest %s to lite engine", req.GetDataDir()))
	_, err = client.Client().UploadCg(ctx, req)
	if err != nil {
		return errors.Wrap(err, "failed to upload cg to ti server")
	}
	return nil
}

func collectTestReports(ctx context.Context, reports []*pb.Report, stepID string, log *zap.SugaredLogger) error {
	// Test cases from reports are identified at a per-step level and won't cause overwriting/clashes
	// at the backend.
	if len(reports) == 0 {
		return nil
	}
	// Create TI proxy client (lite engine)
	client, err := grpcclient.NewTiProxyClient(consts.LiteEnginePort, log)
	if err != nil {
		return err
	}
	defer client.CloseConn()
	repo, _ := external.GetRepo()             // Add repo if it exists, otherwise keep it empty
	sha, _ := external.GetSha()               // Add sha if it exists, otherwise keep it empty
	commitLink, _ := external.GetCommitLink() // Add commit link if it exists, otherwise keep it empty
	for _, report := range reports {
		var rep testreports.TestReporter
		var err error

		x := report.GetType() // pass in report type in proto when other reports are reqd
		switch x {
		case pb.Report_UNKNOWN:
			return errors.New("report type is unknown")
		case pb.Report_JUNIT:
			rep = newJunit(report.GetPaths(), log)
		}

		var tests []string
		testc := rep.GetTests(ctx)
		for t := range testc {
			jt, _ := json.Marshal(t)
			tests = append(tests, string(jt))
		}

		if len(tests) == 0 {
			return nil // We're not erroring even if we can't find any tests to report
		}

		stream, err := client.Client().WriteTests(ctx, grpc_retry.Disable())
		if err != nil {
			return err
		}
		var curr []string
		for _, t := range tests {
			curr = append(curr, t)
			if len(curr)%batchSize == 0 {
				in := &pb.WriteTestsRequest{StepId: stepID, Tests: curr, Repo: repo, Sha: sha, CommitLink: commitLink}
				if serr := stream.Send(in); serr != nil {
					log.Errorw("write tests RPC failed", zap.Error(serr))
				}
				curr = []string{} // ignore RPC failures, try to write whatever you can
			}
		}
		if len(curr) > 0 {
			in := &pb.WriteTestsRequest{StepId: stepID, Tests: curr, Repo: repo, Sha: sha, CommitLink: commitLink}
			if serr := stream.Send(in); serr != nil {
				log.Errorw("write tests RPC failed", zap.Error(serr))
			}
			curr = []string{}
		}

		// Close the stream and receive result
		_, err = stream.CloseAndRecv()
		if err != nil {
			return err
		}
	}
	return nil
}

// selectTests takes a list of files which were changed as input and gets the tests
// to be run corresponding to that.
func selectTests(ctx context.Context, files []types.File, runSelected bool, stepID string, log *zap.SugaredLogger, fs filesystem.FileSystem) (types.SelectTestsResp, error) {
	res := types.SelectTestsResp{}
	isManual := external.IsManualExecution()
	repo, err := external.GetRepo()
	if err != nil {
		return res, err
	}
	// For webhook executions, all the below variables should be set
	sha, err := external.GetSha()
	if err != nil && !isManual {
		return res, err
	}
	source, err := external.GetSourceBranch()
	if err != nil && !isManual {
		return res, err
	}
	target, err := external.GetTargetBranch()
	if err != nil && !isManual {
		return res, err
	} else if isManual {
		target, err = external.GetBranch()
		if err != nil {
			return res, err
		}
	}
	// Create TI proxy client (lite engine)
	client, err := grpcclient.NewTiProxyClient(consts.LiteEnginePort, log)
	if err != nil {
		return res, err
	}
	defer client.CloseConn()
	// Get TI config
	ticonfig, err := getTiConfig(fs)
	if err != nil {
		return res, err
	}
	b, err := json.Marshal(&types.SelectTestsReq{SelectAll: !runSelected, Files: files, TiConfig: ticonfig})
	if err != nil {
		return res, err
	}
	req := &pb.SelectTestsRequest{
		StepId:       stepID,
		Repo:         repo,
		Sha:          sha,
		SourceBranch: source,
		TargetBranch: target,
		Body:         string(b),
	}
	resp, err := client.Client().SelectTests(ctx, req)
	if err != nil {
		return types.SelectTestsResp{}, err
	}
	var selection types.SelectTestsResp
	err = json.Unmarshal([]byte(resp.Selected), &selection)
	if err != nil {
		log.Errorw("could not unmarshal select tests response on addon", zap.Error(err))
		return types.SelectTestsResp{}, err
	}
	return selection, nil
}

func getTiConfig(fs filesystem.FileSystem) (types.TiConfig, error) {
	wrkspcPath, err := external.GetWrkspcPath()
	res := types.TiConfig{}
	if err != nil {
		return res, errors.Wrap(err, "could not get ti config")
	}
	path := fmt.Sprintf("%s/%s", wrkspcPath, tiConfigPath)
	_, err = os.Stat(path)
	if os.IsNotExist(err) {
		return res, nil
	}
	var data []byte
	err = fs.ReadFile(path, func(r io.Reader) error {
		data, err = ioutil.ReadAll(r)
		return err
	})
	if err != nil {
		return res, errors.Wrap(err, "could not read ticonfig file")
	}
	err = yaml.Unmarshal(data, &res)
	if err != nil {
		return res, errors.Wrap(err, "could not unmarshal ticonfig file")
	}
	return res, nil
}
