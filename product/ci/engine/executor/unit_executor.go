package executor

import (
	"context"
	"encoding/base64"
	"fmt"

	"github.com/golang/protobuf/proto"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/ci/engine/steps"
	"go.uber.org/zap"
)

var (
	saveCacheStep        = steps.NewSaveCacheStep
	restoreCacheStep     = steps.NewRestoreCacheStep
	publishArtifactsStep = steps.NewPublishArtifactsStep
	runStep              = steps.NewRunStep
)

//go:generate mockgen -source unit_executor.go -package=executor -destination mocks/unit_executor_mock.go UnitExecutor

// UnitExecutor represents an interface to execute a unit step
type UnitExecutor interface {
	Run(ctx context.Context, step *pb.UnitStep) error
}

type unitExecutor struct {
	log         *zap.SugaredLogger
	stepLogPath string // File path to store logs of step
	tmpFilePath string // File path to store generated temporary files
}

// NewUnitExecutor creates a unit step executor
func NewUnitExecutor(stepLogPath, tmpFilePath string, log *zap.SugaredLogger) UnitExecutor {
	return &unitExecutor{
		stepLogPath: stepLogPath,
		tmpFilePath: tmpFilePath,
		log:         log,
	}
}

func (e *unitExecutor) validate(step *pb.UnitStep) error {
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

// Executes a unit step
func (e *unitExecutor) Run(ctx context.Context, step *pb.UnitStep) error {
	// Ensure step_id is present as a parameter for all logs
	e.log = e.log.With(zap.String("step_id", step.GetId()))
	fs := filesystem.NewOSFileSystem(e.log)
	if err := e.validate(step); err != nil {
		return err
	}

	switch x := step.GetStep().(type) {
	case *pb.UnitStep_Run:
		e.log.Infow("Run step info", "step", x.Run.String())
		if err := runStep(step, e.stepLogPath, e.tmpFilePath, fs, e.log).Run(ctx); err != nil {
			return err
		}
	case *pb.UnitStep_SaveCache:
		e.log.Infow("Save cache step info", "step", x.SaveCache.String())
		if err := saveCacheStep(step, e.tmpFilePath, fs, e.log).Run(ctx); err != nil {
			return err
		}
	case *pb.UnitStep_RestoreCache:
		e.log.Infow("Restore cache step info", "step", x.RestoreCache.String())
		if err := restoreCacheStep(step, e.tmpFilePath, fs, e.log).Run(ctx); err != nil {
			return err
		}
	case *pb.UnitStep_PublishArtifacts:
		e.log.Infow("Publishing artifact info", "step", x.PublishArtifacts.String())
		if err := publishArtifactsStep(step, e.log).Run(ctx); err != nil {
			return err
		}
	case nil:
		e.log.Infow("Field is not set", "step", x)
	default:
		return fmt.Errorf("UnitStep has unexpected type %T", x)
	}

	return nil
}

// decodeUnitStep decodes base64 encoded unit step
func decodeUnitStep(encodedStep string, log *zap.SugaredLogger) (*pb.UnitStep, error) {
	decodedStep, err := base64.StdEncoding.DecodeString(encodedStep)
	if err != nil {
		log.Errorw("Failed to decode step", "encoded_step", encodedStep, zap.Error(err))
		return nil, err
	}

	step := &pb.UnitStep{}
	err = proto.Unmarshal(decodedStep, step)
	if err != nil {
		log.Errorw("Failed to deserialize step", "decoded_step", decodedStep, zap.Error(err))
		return nil, err
	}

	log.Infow("Deserialized step", "step", step.String())
	return step, nil
}

// ExecuteStep executes a unit step of a stage
func ExecuteStep(input, logpath, tmpFilePath string, log *zap.SugaredLogger) {
	step, err := decodeUnitStep(input, log)
	if err != nil {
		log.Fatalw(
			"error while executing step",
			"embedded_stage", input,
			"log_path", logpath,
			"step_id", step.GetId(),
			zap.Error(err),
		)
	}

	ctx := context.Background()
	executor := NewUnitExecutor(logpath, tmpFilePath, log)
	if err := executor.Run(ctx, step); err != nil {
		log.Fatalw(
			"error while executing step",
			"step", step.String(),
			"log_path", logpath,
			"step_id", step.GetId(),
			zap.Error(err),
		)
	}
}
