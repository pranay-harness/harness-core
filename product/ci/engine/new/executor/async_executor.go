package executor

import (
	"context"

	"github.com/wings-software/portal/product/ci/engine/new/state"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

var (
	newStepExecutor = NewStepExecutor
)

// ExecuteStepInAsync executes a step asynchronously.
// It starts execution of a step in a goroutine. Status of step is sent to
// delegate agent service after execution of step finishes.
func ExecuteStepInAsync(ctx context.Context, in *pb.ExecuteStepRequest,
	log *zap.SugaredLogger) {

	go func() {
		s := state.ExecutionState()
		executionID := in.GetExecutionId()
		if s.CanRun(executionID) {
			executeStep(ctx, in, log)
		} else {
			log.Infow("Job is already running with same execution ID",
				"id", executionID, "arg", in)
		}
	}()
}

// Execute a step
func executeStep(ctx context.Context, in *pb.ExecuteStepRequest,
	log *zap.SugaredLogger) {
	e := newStepExecutor(in.GetTmpFilePath(), log)
	err := e.Run(ctx, in.GetStep())
	if err != nil {
		log.Errorw("Job failed with execution ID",
			"id", in.GetExecutionId(), "arg", in, zap.Error(err))
	} else {
		log.Infow("Successfully finished job with execution ID",
			"id", in.GetExecutionId(), "arg", in)
	}
}
