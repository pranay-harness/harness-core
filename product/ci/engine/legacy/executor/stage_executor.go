// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package executor

import (
	"context"
	"encoding/base64"
	"fmt"

	"github.com/gogo/protobuf/jsonpb"
	"github.com/golang/protobuf/proto"
	"github.com/pkg/errors"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/engine/legacy/state"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

// StageExecutor represents an interface to execute a stage
type StageExecutor interface {
	Run() error
}

// NewStageExecutor creates a stage executor
func NewStageExecutor(encodedStage, tmpFilePath string, svcPorts []uint, debug bool,
	log *zap.SugaredLogger) StageExecutor {
	o := make(output.StageOutput)
	unitExecutor := NewUnitExecutor(tmpFilePath, log)
	parallelExecutor := NewParallelExecutor(tmpFilePath, log)
	return &stageExecutor{
		encodedStage:     encodedStage,
		tmpFilePath:      tmpFilePath,
		svcPorts:         svcPorts,
		debug:            debug,
		stageOutput:      o,
		unitExecutor:     unitExecutor,
		parallelExecutor: parallelExecutor,
		log:              log,
	}
}

type stageExecutor struct {
	log              *zap.SugaredLogger
	tmpFilePath      string             // File path to store generated temporary files
	encodedStage     string             // Stage in base64 encoded format
	svcPorts         []uint             // grpc service ports of integration service containers
	debug            bool               // If true, enables debug mode for checking run step logs by not exiting lite-engine
	stageOutput      output.StageOutput // Stage output will store the output of steps in a stage
	unitExecutor     UnitExecutor
	parallelExecutor ParallelExecutor
}

// Executes steps in a stage
func (e *stageExecutor) Run() error {
	ctx := context.Background()
	if e.debug == true {
		defer func() { select {} }()
	}

	// Cleanup all the integration service resources at the end
	defer func() {
		for _, port := range e.svcPorts {
			e.stopIntegrationSvc(ctx, port)
		}
	}()

	execution, err := e.decodeStage(e.encodedStage)
	if err != nil {
		return err
	}

	// Execute steps in a stage & cleans up the resources allocated for a step after its completion.
	cleanupOnly := false
	var stepExecErr error
	for _, step := range execution.GetSteps() {
		if !cleanupOnly {
			e.waitForRunningState()
			stepExecErr = e.executeStep(ctx, step, execution.GetAccountId())
			if stepExecErr != nil {
				cleanupOnly = true
			}
		}
		e.cleanupStep(ctx, step)
	}
	return stepExecErr
}

// waits for execution state to be in running state
func (e *stageExecutor) waitForRunningState() {
	s := state.ExecutionState()
	stateVal := s.GetState()
	e.log.Infow("Current execution state", "state", state.ExecutionStateStr(stateVal))
	if stateVal == state.RUNNING {
		return
	}

	// Stage is paused. Wait for resume signal to proceed.
	ch := s.ResumeSignal()
	for {
		switch {
		case <-ch:
			if s.GetState() == state.RUNNING {
				return
			}
		}
	}
}

// executeStep method executes a unit step or a parallel step
func (e *stageExecutor) executeStep(ctx context.Context, step *pb.Step, accountID string) error {
	switch x := step.GetStep().(type) {
	case *pb.Step_Unit:
		stepOutput, err := e.unitExecutor.Run(ctx, step.GetUnit(), e.stageOutput, accountID)
		if err != nil {
			return err
		}
		e.stageOutput[step.GetUnit().GetId()] = stepOutput
	case *pb.Step_Parallel:
		stepOutputByID, err := e.parallelExecutor.Run(ctx, step.GetParallel(), e.stageOutput, accountID)
		if err != nil {
			return err
		}
		for stepID, stepOutput := range stepOutputByID {
			e.stageOutput[stepID] = stepOutput
		}
	default:
		return fmt.Errorf("Step has unexpected type %T", x)
	}
	return nil
}

// cleanupStep method terminates any resource present for the step e.g. addon container
func (e *stageExecutor) cleanupStep(ctx context.Context, step *pb.Step) error {
	var err error
	switch x := step.GetStep().(type) {
	case *pb.Step_Unit:
		err = e.unitExecutor.Cleanup(ctx, step.GetUnit())
	case *pb.Step_Parallel:
		err = e.parallelExecutor.Cleanup(ctx, step.GetParallel())
	default:
		err = fmt.Errorf("Step has unexpected type %T", x)
	}
	return err
}

// Stops CI-Addon GRPC server running on integration service container
func (e *stageExecutor) stopIntegrationSvc(ctx context.Context, port uint) error {
	addonClient, err := newAddonClient(port, e.log)
	if err != nil {
		e.log.Errorw("Could not create CI Addon client running integration service",
			"port", port, zap.Error(err))
		return errors.Wrap(err, "Could not create CI Addon client")
	}
	defer addonClient.CloseConn()

	_, err = addonClient.Client().SignalStop(ctx, &addonpb.SignalStopRequest{})
	if err != nil {
		e.log.Errorw("Unable to send Stop server request to addon running integration service",
			"port", port, zap.Error(err))
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

	e.log.Infow("Deserialized execution", "execution", msgToStr(execution, e.log))
	return execution, nil
}

// ExecuteStage executes a stage of the pipeline
func ExecuteStage(input, tmpFilePath string, svcPorts []uint, debug bool, log *zap.SugaredLogger) error {
	executor := NewStageExecutor(input, tmpFilePath, svcPorts, debug, log)
	if err := executor.Run(); err != nil {
		log.Errorw(
			"error while executing steps in a stage",
			"embedded_stage", input,
			zap.Error(err),
		)
		return err
	}
	return nil
}

func msgToStr(msg *pb.Execution, log *zap.SugaredLogger) string {
	m := jsonpb.Marshaler{}
	jsonMsg, err := m.MarshalToString(msg)
	if err != nil {
		log.Errorw("failed to convert stage to json", zap.Error(err))
		return msg.String()
	}
	return jsonMsg
}
