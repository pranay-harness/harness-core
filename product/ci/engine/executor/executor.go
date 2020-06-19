package executor

import (
	"context"
	"encoding/base64"
	"fmt"
	"go.uber.org/zap"

	"github.com/golang/protobuf/proto"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
)

// StageExecutor represents an interface to execute a stage
type StageExecutor interface {
	Run() error
}

// NewStageExecutor creates a stage executor
func NewStageExecutor(encodedStage, stepLogPath string, log *zap.SugaredLogger) StageExecutor {
	return &stageExecutor{
		encodedStage: encodedStage,
		stepLogPath:  stepLogPath,
		log:          log,
	}
}

type stageExecutor struct {
	log          *zap.SugaredLogger
	stepLogPath  string
	encodedStage string
}

// Executes steps in a stage
func (e *stageExecutor) Run() error {
	if e.stepLogPath == "" {
		err := fmt.Errorf("Step log path should be non-empty")
		e.log.Errorw("Empty step log path", zap.Error(err))
		return err
	}

	execution, err := e.decodeStage(e.encodedStage)
	if err != nil {
		return err
	}

	ctx := context.Background()
	fs := filesystem.NewOSFileSystem(e.log)
	for _, step := range execution.GetSteps() {
		if err := e.validateStep(step); err != nil {
			return err
		}

		switch x := step.GetStep().(type) {
		case *pb.Step_Run:
			e.log.Infow("Run step info", "step", x.Run.String())
			if err := NewRunStepExecutor(step, e.stepLogPath, fs, e.log).Run(ctx); err != nil {
				return err
			}
		case *pb.Step_SaveCache:
			e.log.Infow("Save cache step info", "step", x.SaveCache)
		case nil:
			e.log.Infow("Field is not set", "step", x)
		default:
			return fmt.Errorf("Step.Step has unexpected type %T", x)
		}
	}

	return nil
}

func (e *stageExecutor) validateStep(step *pb.Step) error {
	if step.GetId() == "" {
		err := fmt.Errorf("Step ID should be non-empty")
		e.log.Errorw("Step ID is not set", zap.Error(err))
		return err
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
