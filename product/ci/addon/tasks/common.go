package tasks

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"time"

	"github.com/ghodss/yaml"
	grpc_retry "github.com/grpc-ecosystem/go-grpc-middleware/retry"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/metrics"
	"github.com/wings-software/portal/commons/go/lib/utils"
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
	mlog     = metrics.Log
	newJunit = junit.New
)

func runCmd(ctx context.Context, cmd exec.Command, stepID string, commands []string, retryCount int32, startTime time.Time,
	logMetrics bool, log *zap.SugaredLogger, metricLog *zap.SugaredLogger) error {
	err := cmd.Start()
	if err != nil {
		log.Errorw(
			"error encountered while executing the step",
			"step_id", stepID,
			"retry_count", retryCount,
			"commands", commands,
			"elapsed_time_ms", utils.TimeSince(startTime),
			zap.Error(err))
		return err
	}

	if logMetrics {
		pid := cmd.Pid()
		mlog(int32(pid), stepID, metricLog)
	}

	err = cmd.Wait()
	if rusage, e := cmd.ProcessState().SysUsageUnit(); e == nil {
		metricLog.Infow(
			"max RSS memory used by step",
			"step_id", stepID,
			"max_rss_memory_kb", rusage.Maxrss)
	}

	if ctxErr := ctx.Err(); ctxErr == context.DeadlineExceeded {
		log.Errorw(
			"timeout while executing the step",
			"step_id", stepID,
			"retry_count", retryCount,
			"commands", commands,
			"elapsed_time_ms", utils.TimeSince(startTime),
			zap.Error(ctxErr))
		return ctxErr
	}

	if err != nil {
		log.Errorw(
			"error encountered while executing the step",
			"step_id", stepID,
			"retry_count", retryCount,
			"commands", commands,
			"elapsed_time_ms", utils.TimeSince(startTime),
			zap.Error(err))
		return err
	}
	return nil
}

func collectCg(ctx context.Context, stepID, cgDir string, log *zap.SugaredLogger) error {
	repo, err := external.GetRepo()
	if err != nil {
		return err
	}
	sha, err := external.GetSha()
	if err != nil {
		return err
	}
	branch, err := external.GetSourceBranch()
	if err != nil {
		return err
	}
	// Create TI proxy client (lite engine)
	client, err := grpcclient.NewTiProxyClient(consts.LiteEnginePort, log)
	if err != nil {
		return err
	}
	defer client.CloseConn()
	req := &pb.UploadCgRequest{
		StepId: stepID,
		Repo:   repo,
		Sha:    sha,
		Branch: branch,
		CgDir:  cgDir,
	}
	log.Infow(fmt.Sprintf("sending cgRequest %s to lite engine", req.GetCgDir()))
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
		testc, _ := rep.GetTests(ctx)
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
				in := &pb.WriteTestsRequest{StepId: stepID, Tests: curr}
				if serr := stream.Send(in); serr != nil {
					log.Errorw("write tests RPC failed", zap.Error(serr))
				}
				curr = []string{} // ignore RPC failures, try to write whatever you can
			}
		}
		if len(curr) > 0 {
			in := &pb.WriteTestsRequest{StepId: stepID, Tests: curr}
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
func selectTests(ctx context.Context, files []types.File, stepID string, log *zap.SugaredLogger, fs filesystem.FileSystem) (types.SelectTestsResp, error) {
	res := types.SelectTestsResp{}
	if len(files) == 0 {
		// No files changed, don't do anything
		return res, nil
	}
	repo, err := external.GetRepo()
	if err != nil {
		return res, err
	}
	sha, err := external.GetSha()
	if err != nil {
		return res, err
	}
	branch, err := external.GetSourceBranch()
	if err != nil {
		return res, err
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
	b, err := json.Marshal(&types.SelectTestsReq{Files: files, TiConfig: ticonfig})
	if err != nil {
		return res, err
	}
	req := &pb.SelectTestsRequest{
		StepId: stepID,
		Repo:   repo,
		Sha:    sha,
		Branch: branch,
		Body:   string(b),
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
