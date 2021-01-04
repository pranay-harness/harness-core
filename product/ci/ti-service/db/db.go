// Package db defines a DB interface for tests
package db

import (
	"context"
	"github.com/wings-software/portal/product/ci/ti-service/types"
)

// Db defines the db interface to perform operations on tests
type Db interface {
	// Write writes testcases to the underlying DB.
	Write(ctx context.Context, table, accountID, orgId, projectId, pipelineId, buildId, stageId, stepId,
		report string, tests ...*types.TestCase) error

	// Summary provides a high-level test case summary
	Summary(ctx context.Context, table, accountID, orgId, projectId, pipelineId, buildId, report string) (types.SummaryResponse, error)

	// GetTestCases returns test cases corresponding to a specific suite
	GetTestCases(ctx context.Context, table, accountID, orgId, projectId, pipelineId, buildId,
		report, suiteName, sortAttribute, status, order, limit, offset string) (types.TestCases, error)

	// GetTestSuites returns suite-level details for the tests
	GetTestSuites(ctx context.Context, table, accountID, orgId, projectId, pipelineId, buildId,
		report, sortAttribute, status, order, limit, offset string) (types.TestSuites, error)
}
