package logs

import (
	"go.uber.org/zap"
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
