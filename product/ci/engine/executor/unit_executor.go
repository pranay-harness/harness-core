package executor

import (
	"context"
	"encoding/base64"
	"fmt"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/ci/engine/status"
	"github.com/wings-software/portal/product/ci/engine/steps"
	"go.uber.org/zap"
)

var (
	saveCacheStep        = steps.NewSaveCacheStep
	restoreCacheStep     = steps.NewRestoreCacheStep
	publishArtifactsStep = steps.NewPublishArtifactsStep
	runStep              = steps.NewRunStep
	sendStepStatus       = status.SendStepStatus
)

//go:generate mockgen -source unit_executor.go -package=executor -destination mocks/unit_executor_mock.go UnitExecutor

// UnitExecutor represents an interface to execute a unit step
type UnitExecutor interface {
	Run(ctx context.Context, step *pb.UnitStep, so output.StageOutput, accountID string) (*output.StepOutput, error)
}

type unitExecutor struct {
	stepLogPath string // File path to store logs of step
	tmpFilePath string // File path to store generated temporary files
	log         *zap.SugaredLogger
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
	if step.GetCallbackToken() == "" {
		err := fmt.Errorf("Callback token should be non-empty")
		e.log.Errorw("Callback token is not set", zap.Error(err))
		return err
	}
	if step.GetTaskId() == "" {
		err := fmt.Errorf("Task ID should be non-empty")
		e.log.Errorw("Task ID is not set", zap.Error(err))
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
func (e *unitExecutor) Run(ctx context.Context, step *pb.UnitStep, so output.StageOutput,
	accountID string) (*output.StepOutput, error) {
	// Ensure step_id is present as a parameter for all logs
	e.log = e.log.With(zap.String("step_id", step.GetId()))

	start := time.Now()
	stepOutput, numRetries, err := e.execute(ctx, step, so)
	timeTaken := time.Since(start)

	callbackToken := step.GetCallbackToken()
	taskID := step.GetTaskId()
	statusErr := sendStepStatus(ctx, accountID, callbackToken, taskID, numRetries, timeTaken,
		stepOutput, err, e.log)
	if statusErr != nil {
		e.log.Errorw("Failed to send step status. Bailing out stage execution", zap.Error(err))
		return nil, statusErr
	}
	return stepOutput, err
}

func (e *unitExecutor) execute(ctx context.Context, step *pb.UnitStep,
	so output.StageOutput) (*output.StepOutput, int32, error) {
	numRetries := int32(1)
	var err error
	var stepOutput *output.StepOutput

	fs := filesystem.NewOSFileSystem(e.log)
	if err = e.validate(step); err != nil {
		return nil, numRetries, err
	}

	switch x := step.GetStep().(type) {
	case *pb.UnitStep_Run:
		e.log.Infow("Run step info", "step", x.Run.String())
		stepOutput, numRetries, err = runStep(step, e.stepLogPath, e.tmpFilePath, so, fs, e.log).Run(ctx)
		if err != nil {
			return nil, numRetries, err
		}
	case *pb.UnitStep_SaveCache:
		e.log.Infow("Save cache step info", "step", x.SaveCache.String())
		if err = saveCacheStep(step, e.tmpFilePath, so, fs, e.log).Run(ctx); err != nil {
			return nil, numRetries, err
		}
	case *pb.UnitStep_RestoreCache:
		e.log.Infow("Restore cache step info", "step", x.RestoreCache.String())
		if err = restoreCacheStep(step, e.tmpFilePath, so, fs, e.log).Run(ctx); err != nil {
			return nil, numRetries, err
		}
	case *pb.UnitStep_PublishArtifacts:
		e.log.Infow("Publishing artifact info", "step", x.PublishArtifacts.String())
		if err = publishArtifactsStep(step, so, e.log).Run(ctx); err != nil {
			return nil, numRetries, err
		}
	case nil:
		e.log.Infow("Field is not set", "step", x)
	default:
		return nil, numRetries, fmt.Errorf("UnitStep has unexpected type %T", x)
	}

	return stepOutput, numRetries, nil
}

// decodeExecuteStepRequest decodes base64 encoded unit step
func decodeExecuteStepRequest(encodedStep string, log *zap.SugaredLogger) (*pb.ExecuteStepRequest, error) {
	decodedStep, err := base64.StdEncoding.DecodeString(encodedStep)
	if err != nil {
		log.Errorw("Failed to decode step", "encoded_step", encodedStep, zap.Error(err))
		return nil, err
	}

	r := &pb.ExecuteStepRequest{}
	err = proto.Unmarshal(decodedStep, r)
	if err != nil {
		log.Errorw("Failed to deserialize step", "decoded_step", decodedStep, zap.Error(err))
		return nil, err
	}

	log.Infow("Deserialized step", "step", r.GetStep().String())
	return r, nil
}

// ExecuteStep executes a unit step of a stage
func ExecuteStep(input, logpath, tmpFilePath string, log *zap.SugaredLogger) {
	r, err := decodeExecuteStepRequest(input, log)
	if err != nil {
		log.Fatalw(
			"error while executing step",
			"embedded_stage", input,
			"log_path", logpath,
			"step_id", r.GetStep().GetId(),
			zap.Error(err),
		)
	}

	ctx := context.Background()
	executor := NewUnitExecutor(logpath, tmpFilePath, log)

	stageOutput := make(output.StageOutput)
	for _, stepOutput := range r.GetStageOutput().GetStepOutputs() {
		stageOutput[stepOutput.GetStepId()] = &output.StepOutput{Output: stepOutput.GetOutput()}
	}
	if _, err := executor.Run(ctx, r.GetStep(), stageOutput, r.GetAccountId()); err != nil {
		log.Fatalw(
			"error while executing step",
			"step", r.GetStep().String(),
			"log_path", logpath,
			"step_id", r.GetStep().GetId(),
			zap.Error(err),
		)
	}
}
