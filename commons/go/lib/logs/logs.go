// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package logs

import (
	"fmt"
	"os"

	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"go.uber.org/zap/zaptest/observer"
)

// GetLogger returns a configured zap.Logger configuration
func GetLogger(deployment string, deployable string, taskID string, customerID string, sessionID string, applicationID string, verbose bool) (*zap.Logger, error) {
	return NewBuilder().
		Verbose(verbose).
		WithDeployment(deployment).
		WithFields("task_id", taskID, "deployable", deployable, "customer_id", customerID,
			"session_id", sessionID, "application_id", applicationID).
		Build()
}

// GetObservedLogger returns *zap.Logger and corresponding in memory recorded observed logs
// for testing logging events in unit tests
func GetObservedLogger(enab zapcore.LevelEnabler) (*zap.Logger, *observer.ObservedLogs) {
	core, observedLogs := observer.New(enab)
	obsOption := zap.WrapCore(func(zapcore.Core) zapcore.Core {
		return core
	})
	logger, err := zap.NewDevelopment()
	if err != nil {
		fmt.Println("error initializing logger", err)
		os.Exit(1)
	}
	l := logger.WithOptions(obsOption)
	return l, observedLogs
}
