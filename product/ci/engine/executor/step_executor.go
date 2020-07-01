package executor

import (
	"context"
	"encoding/base64"
	"fmt"
	"go.uber.org/zap"

	"github.com/golang/protobuf/proto"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/ci/engine/steps"
)

var (
	saveCacheStep    = steps.NewSaveCacheStep
	restoreCacheStep = steps.NewRestoreCacheStep
)

// StepExecutor represents an interface to execute a step
type StepExecutor interface {
	Run(ctx context.Context, step *pb.Step) error
}

type stepExecutor struct {
	log         *zap.SugaredLogger
	stepLogPath string // File path to store logs of step
	tmpFilePath string // File path to store generated temporary files
}

// NewStepExecutor creates a step executor
func NewStepExecutor(stepLogPath, tmpFilePath string, log *zap.SugaredLogger) StepExecutor {
	return &stepExecutor{
		stepLogPath: stepLogPath,
		tmpFilePath: tmpFilePath,
		log:         log,
	}
}

func (e *stepExecutor) validate(step *pb.Step) error {
	if step.GetId() == "" {
		err := fmt.Errorf("Step ID should be non-empty")
		e.log.Errorw("Step ID is not set", zap.Error(err))
		return err
	}
	if e.stepLogPath == "" {
		err := fmt.Errorf("Step log path should be non-empty")
		e.log.Errorw("Empty step log path", zap.Error(err))
		return err
	}
	return nil
}

// Executes a step
func (e *stepExecutor) Run(ctx context.Context, step *pb.Step) error {
	fs := filesystem.NewOSFileSystem(e.log)
	if err := e.validate(step); err != nil {
		return err
	}

	switch x := step.GetStep().(type) {
	case *pb.Step_Run:
		e.log.Infow("Run step info", "step", x.Run.String())
		if err := steps.NewRunStep(step, e.stepLogPath, fs, e.log).Run(ctx); err != nil {
			return err
		}
	case *pb.Step_SaveCache:
		e.log.Infow("Save cache step info", "step", x.SaveCache)
		if err := saveCacheStep(step, e.tmpFilePath, fs, e.log).Run(ctx); err != nil {
			return err
		}
	case *pb.Step_RestoreCache:
		e.log.Infow("Restore cache step info", "step", x.RestoreCache)
		if err := restoreCacheStep(step, e.tmpFilePath, fs, e.log).Run(ctx); err != nil {
			return err
		}
	case nil:
		e.log.Infow("Field is not set", "step", x)
	default:
		return fmt.Errorf("Step.Step has unexpected type %T", x)
	}

	return nil
}

// DecodeStep decodes base64 encoded step
func DecodeStep(encodedStep string, log *zap.SugaredLogger) (*pb.Step, error) {
	decodedStep, err := base64.StdEncoding.DecodeString(encodedStep)
	if err != nil {
		log.Errorw("Failed to decode step", "encoded_step", encodedStep, zap.Error(err))
		return nil, err
	}

	step := &pb.Step{}
	err = proto.Unmarshal(decodedStep, step)
	if err != nil {
		log.Errorw("Failed to deserialize step", "decoded_step", decodedStep, zap.Error(err))
		return nil, err
	}

	log.Infow("Deserialized step", "step", step.String())
	return step, nil
}

// ExecuteStep executes a step of a stage
func ExecuteStep(input, logpath, tmpFilePath string, log *zap.SugaredLogger) {
	step, err := DecodeStep(input, log)
	if err != nil {
		log.Fatalw(
			"error while executing step",
			"embedded_stage", input,
			"log_path", logpath,
			zap.Error(err),
		)
	}

	ctx := context.Background()
	executor := NewStepExecutor(logpath, tmpFilePath, log)
	if err := executor.Run(ctx, step); err != nil {
		log.Fatalw(
			"error while executing step",
			"step", step.String(),
			"log_path", logpath,
			zap.Error(err),
		)
	}
}
