package executor

import (
	"context"
	"encoding/base64"

	"github.com/golang/protobuf/proto"
	"github.com/pkg/errors"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/engine/grpc"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

var (
	newCIAddonClient = grpc.NewCIAddonClient
)

// StageExecutor represents an interface to execute a stage
type StageExecutor interface {
	Run() error
}

// NewStageExecutor creates a stage executor
func NewStageExecutor(encodedStage, stepLogPath, tmpFilePath string, log *zap.SugaredLogger) StageExecutor {
	return &stageExecutor{
		encodedStage: encodedStage,
		stepLogPath:  stepLogPath,
		tmpFilePath:  tmpFilePath,
		log:          log,
	}
}

type stageExecutor struct {
	log          *zap.SugaredLogger
	stepLogPath  string // File path to store logs of steps
	tmpFilePath  string // File path to store generated temporary files
	encodedStage string // Stage in base64 encoded format
}

// Executes steps in a stage
func (e *stageExecutor) Run() error {
	ctx := context.Background()
	defer e.stopAddonServer(ctx)

	execution, err := e.decodeStage(e.encodedStage)
	if err != nil {
		return err
	}

	stepExecutor := NewStepExecutor(e.stepLogPath, e.tmpFilePath, e.log)
	for _, step := range execution.GetSteps() {
		err := stepExecutor.Run(ctx, step)
		if err != nil {
			return err
		}
	}
	return nil
}

// Stop CI-Addon GRPC server
func (e *stageExecutor) stopAddonServer(ctx context.Context) error {
	ciAddonClient, err := newCIAddonClient(grpc.CIAddonPort, e.log)
	if err != nil {
		return errors.Wrap(err, "Could not create CI Addon client")
	}
	defer ciAddonClient.CloseConn()

	_, err = ciAddonClient.Client().SignalStop(ctx, &addonpb.SignalStopRequest{})
	if err != nil {
		e.log.Warnw("Unable to send Stop server request", "error_msg", zap.Error(err))
		return errors.Wrap(err, "Could not send stop server request")
	}
	return nil
}

func (e *stageExecutor) decodeStage(encodedStage string) (*pb.Execution, error) {
	decodedStage, err := base64.StdEncoding.DecodeString(e.encodedStage)
	if err != nil {
		e.log.Errorw("Failed to decode stage", "encoded_stage", e.encodedStage, zap.Error(err))
		return nil, err
	}

	execution := &pb.Execution{}
	err = proto.Unmarshal(decodedStage, execution)
	if err != nil {
		e.log.Errorw("Failed to deserialize stage", "decoded_stage", decodedStage, zap.Error(err))
		return nil, err
	}

	e.log.Infow("Deserialized execution", "execution", execution.String())
	return execution, nil
}

// ExecuteStage executes a stage of the pipeline
func ExecuteStage(input, logpath, tmpFilePath string, log *zap.SugaredLogger) {
	executor := NewStageExecutor(input, logpath, tmpFilePath, log)
	if err := executor.Run(); err != nil {
		log.Fatalw(
			"error while executing steps in a stage",
			"embedded_stage", input,
			"log_path", logpath,
			zap.Error(err),
		)
	}
}
