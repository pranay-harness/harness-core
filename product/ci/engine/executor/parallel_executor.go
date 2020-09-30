package executor

import (
	"context"
	"time"

	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

//go:generate mockgen -source parallel_executor.go -package=executor -destination mocks/parallel_executor_mock.go ParallelExecutor

// ParallelExecutor represents an interface to execute a parallel step
type ParallelExecutor interface {
	Run(ctx context.Context, step *pb.ParallelStep, so output.StageOutput, accountID string) (map[string]*output.StepOutput, error)
	Cleanup(ctx context.Context, ps *pb.ParallelStep) error
}

type parallelExecutor struct {
	tmpFilePath  string // File path to store generated temporary files
	log          *zap.SugaredLogger
	unitExecutor UnitExecutor
}

type unitStepResponse struct {
	stepID     string
	stepOutput *output.StepOutput
	err        error
}

// NewParallelExecutor creates a parallel step executor
func NewParallelExecutor(tmpFilePath string, log *zap.SugaredLogger) ParallelExecutor {
	unitExecutor := NewUnitExecutor(tmpFilePath, log)
	return &parallelExecutor{
		tmpFilePath:  tmpFilePath,
		unitExecutor: unitExecutor,
		log:          log,
	}
}

// Executes a parallel step by executing all the steps in parallel.
func (e *parallelExecutor) Run(ctx context.Context, ps *pb.ParallelStep, so output.StageOutput,
	accountID string) (map[string]*output.StepOutput, error) {
	start := time.Now()
	if err := e.validate(ps); err != nil {
		return nil, err
	}

	// Figure out the run step tasks to run in parallel
	numSteps := len(ps.GetSteps())
	tasks := make(chan *pb.UnitStep, numSteps)
	results := make(chan unitStepResponse, numSteps)
	for _, step := range ps.GetSteps() {
		s := step
		go func() {
			stepOutput, err := e.unitExecutor.Run(ctx, s, so, accountID)
			results <- unitStepResponse{
				stepID:     s.GetId(),
				stepOutput: stepOutput,
				err:        err,
			}
		}()
	}
	close(tasks)

	stepOutputByID := make(map[string]*output.StepOutput)
	// Evaluate the parallel step results
	for i := 0; i < numSteps; i++ {
		result := <-results
		if result.err != nil {
			e.log.Errorw(
				"failed to execute parallel step",
				"step_id", ps.GetId(),
				"elapsed_time_ms", utils.TimeSince(start),
				zap.Error(result.err),
			)
			return nil, result.err
		}

		stepOutputByID[result.stepID] = result.stepOutput
	}

	e.log.Infow(
		"Successfully executed parallel step",
		"step_id", ps.GetId(),
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return stepOutputByID, nil
}

// Validates parallel step
func (e *parallelExecutor) validate(ps *pb.ParallelStep) error {
	if ps.GetId() == "" {
		err := errors.New("Parallel step ID should be non-empty")
		e.log.Errorw("Parallel step ID is not set", zap.Error(err))
		return err
	}
	return nil
}

func (e *parallelExecutor) Cleanup(ctx context.Context, ps *pb.ParallelStep) error {
	var cleanupErr error
	for _, step := range ps.GetSteps() {
		err := e.unitExecutor.Cleanup(ctx, step)
		if err != nil {
			cleanupErr = err
		}
	}
	return cleanupErr
}
