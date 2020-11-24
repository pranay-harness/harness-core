package services

import (
	"context"
	"fmt"
	"io"
	"os"
	"time"

	"github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/images"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/addon/imageutil"
	"go.uber.org/zap"
)

//go:generate mockgen -source service.go -package=services -destination mocks/service_mock.go IntegrationSvc

const (
	imageSecretEnv = "HARNESS_DOCKER_SECRET" // Docker image secret for integration service
)

var (
	getImgMetadata = imageutil.GetEntrypoint
)

// IntegrationSvc represents interface to execute an integration service
type IntegrationSvc interface {
	Run() error
}

type integrationSvc struct {
	id         string
	image      string
	entrypoint []string
	args       []string
	log        *zap.SugaredLogger
	procWriter io.Writer
	cmdFactory exec.CommandFactory
}

// NewIntegrationSvc creates a integration service executor
func NewIntegrationSvc(svcID, image string, entrypoint, args []string, log *zap.SugaredLogger, w io.Writer) IntegrationSvc {
	return &integrationSvc{
		id:         svcID,
		image:      image,
		entrypoint: entrypoint,
		args:       args,
		cmdFactory: exec.OsCommand(),
		log:        log,
		procWriter: w,
	}
}

// Runs integration test service
func (s *integrationSvc) Run() error {
	start := time.Now()
	ctx := context.Background()
	commands, err := s.getEntrypoint(ctx)
	if err != nil {
		logErr(s.log, "failed to find entrypoint for plugin", s.id, commands, start, err)
		return err
	}

	if len(commands) == 0 {
		err := fmt.Errorf("service entrypoint is empty")
		logErr(s.log, "entrypoint fetched from remote for service is empty", s.id, commands, start, err)
		return err
	}

	cmd := s.cmdFactory.Command(commands[0], commands[1:]...).
		WithStdout(s.procWriter).WithStderr(s.procWriter).WithEnvVarsMap(nil)
	err = cmd.Run()
	if err != nil {
		logErr(s.log, "error encountered while executing integration service", s.id, commands, start, err)
		return err
	}

	s.log.Warnw(
		"Service execution stopped",
		"service_id", s.id,
		"arguments", commands,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}

func (s *integrationSvc) getEntrypoint(ctx context.Context) ([]string, error) {
	if len(s.entrypoint) != 0 && len(s.args) != 0 {
		return images.CombinedEntrypoint(s.entrypoint, s.args), nil
	}

	imageSecret, _ := os.LookupEnv(imageSecretEnv)
	return s.combinedEntrypoint(getImgMetadata(ctx, s.id, s.image, imageSecret, s.log))
}

// combines the entrypoint & commands and returns the combined entrypoint for a docker image.
// It gives priority to user specified entrypoint & args over image entrypoint & commands.
func (s *integrationSvc) combinedEntrypoint(imgEndpoint, imgCmds []string, err error) ([]string, error) {
	if err != nil {
		return nil, err
	}

	ep := imgEndpoint
	if len(s.entrypoint) != 0 {
		ep = s.entrypoint
	}

	cmds := imgCmds
	if len(s.args) != 0 {
		cmds = s.args
	}
	return images.CombinedEntrypoint(ep, cmds), nil
}

func logErr(log *zap.SugaredLogger, errMsg, svcID string, cmds []string, startTime time.Time, err error) {
	log.Errorw(
		errMsg,
		"commands", cmds,
		"id", svcID,
		"elapsed_time_ms", utils.TimeSince(startTime),
		zap.Error(err),
	)
}
