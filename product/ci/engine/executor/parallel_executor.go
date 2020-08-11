package executor

import (
	"context"
	"time"

	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/utils"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

//go:generate mockgen -source parallel_executor.go -package=executor -destination mocks/parallel_executor_mock.go ParallelExecutor

// ParallelExecutor represents an interface to execute a parallel step
type ParallelExecutor interface {
	Run(ctx context.Context, step *pb.ParallelStep) error
}

type parallelExecutor struct {
	stepLogPath  string // File path to store logs of step
	tmpFilePath  string // File path to store generated temporary files
	mainOnly     bool   // Whether to execute all the parallel steps on main lite-engine
	workerPorts  []uint // GRPC server ports for worker lite-engines that are used for executing parallel steps
	log          *zap.SugaredLogger
	unitExecutor UnitExecutor
}

type unitStepResponse struct {
	stepID string
	err    error
}

type worker struct {
	id       int  // Worker identifier
	mainOnly bool // Whether to run this task on main lite-engine or not
	port     uint // Port to send the task. Valid only if mainOnly is set to false.
}

// NewParallelExecutor creates a parallel step executor
func NewParallelExecutor(stepLogPath, tmpFilePath string, workerPorts []uint, log *zap.SugaredLogger) ParallelExecutor {
	unitExecutor := NewUnitExecutor(stepLogPath, tmpFilePath, log)
	mainOnly := len(workerPorts) == 0
	return &parallelExecutor{
		stepLogPath:  stepLogPath,
		tmpFilePath:  tmpFilePath,
		mainOnly:     mainOnly,
		workerPorts:  workerPorts,
		unitExecutor: unitExecutor,
		log:          log,
	}
}

// Executes a parallel step
// 1. Run steps are executed using:
// 	  * If worker lite-engines are present, run steps are executed in parallel in a worker pool composed of
//		main and worker lite-engines
// 	  * Else all run steps are executed in parallel on main lite-engine in separate goroutines.
// 2. All the non-run steps i.e. save/restore & publish artifacts are executed in parallel on main lite-engine.
//	  Publish artifacts is executed on CI-addon. Hence, there is no reason for sending them to worker lite-engines.
//	  Save/restore cache are not memory/cpu intensive tasks. Hence, executing them on main lite-engine.
func (e *parallelExecutor) Run(ctx context.Context, ps *pb.ParallelStep) error {
	start := time.Now()
	if err := e.validate(ps); err != nil {
		return err
	}

	// Figure out the run step tasks to run in parallel
	numSteps := len(ps.GetSteps())
	numRunSteps := 0
	tasks := make(chan *pb.UnitStep, numSteps)
	results := make(chan unitStepResponse, numSteps)
	for _, step := range ps.GetSteps() {
		s := step
		switch s.GetStep().(type) {
		case *pb.UnitStep_Run:
			tasks <- s
			numRunSteps++
		default:
			go func() {
				err := e.unitExecutor.Run(ctx, s)
				results <- unitStepResponse{
					stepID: s.GetId(),
					err:    err,
				}
			}()
		}
	}
	close(tasks)

	// Execute run steps in parallel on lite-engine main and workers.
	workers := e.getWorkers(numRunSteps)
	for _, w := range workers {
		go func(w worker) {
			for t := range tasks {
				var err error
				if w.mainOnly {
					err = e.unitExecutor.Run(ctx, t)
				} else {
					err = e.executeRemoteStep(ctx, w, t)
				}

				results <- unitStepResponse{
					stepID: t.GetId(),
					err:    err,
				}
			}
		}(w)
	}

	// // Evaluate the parallel step results
	for i := 0; i < numSteps; i++ {
		result := <-results
		if result.err != nil {
			e.log.Warnw(
				"failed to execute parallel step",
				"step_id", ps.GetId(),
				"elapsed_time_ms",
				utils.TimeSince(start),
				zap.Error(result.err),
			)
			return result.err
		}
	}

	e.log.Infow(
		"Successfully executed parallel step",
		"step_id", ps.GetId(),
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
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

// Return list of workers to execute the parallel steps.
func (e *parallelExecutor) getWorkers(numRunSteps int) []worker {
	var workers []worker
	if e.mainOnly {
		for i := 0; i < numRunSteps; i++ {
			workers = append(workers, worker{id: i, mainOnly: e.mainOnly})
		}
	} else {
		workers = append(workers, worker{id: 0, mainOnly: true})
		for i, port := range e.workerPorts {
			workers = append(workers, worker{id: i + 1, mainOnly: false, port: port})
		}
	}
	return workers
}

// Create worker lite-engine client and send task to execute run step
func (e *parallelExecutor) executeRemoteStep(ctx context.Context, w worker, step *pb.UnitStep) error {
	c, err := newLiteEngineClient(w.port, e.log)
	if err != nil {
		return errors.Wrap(err, "Could not create worker lite engine client")
	}
	defer c.CloseConn()

	_, err = c.Client().ExecuteStep(ctx, &pb.ExecuteStepRequest{Step: step})
	if err != nil {
		e.log.Warnw("Failed to execute remote step request", "error_msg", zap.Error(err))
		return errors.Wrap(err, "Could not send execute step request")
	}
	return nil
}
