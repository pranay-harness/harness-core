// Package db defines a DB interface for tests
package db

import (
	"context"
	"github.com/wings-software/portal/product/ci/ti-service/types"
)

// Db defines the db interface to perform operations on tests
type Db interface {
	// Ping pings the database to see if it's available
	Ping(ctx context.Context) error

	// Write writes testcases to the underlying DB.
	Write(ctx context.Context, accountID, orgId, projectId, pipelineId, buildId, stageId, stepId,
		report, repo, sha string, tests ...*types.TestCase) error

	// Summary provides a high-level test case summary
	Summary(ctx context.Context, accountID, orgId, projectId, pipelineId, buildId, stepId, stageId, report string) (types.SummaryResponse, error)

	// GetTestCases returns test cases corresponding to a specific suite
	GetTestCases(ctx context.Context, accountID, orgId, projectId, pipelineId, buildId, stepId, stageId,
		report, suiteName, sortAttribute, status, order, limit, offset string) (types.TestCases, error)

	// GetTestSuites returns suite-level details for the tests
	GetTestSuites(ctx context.Context, accountID, orgId, projectId, pipelineId, buildId, stepId, stageId,
		report, sortAttribute, status, order, limit, offset string) (types.TestSuites, error)

	// WriteSelectedTests writes selected test information to the underlying DB.
	WriteSelectedTests(ctx context.Context, accountID, orgId, projectId, pipelineId, buildId,
		stageId, stepId, repo, source, target string, selected types.SelectTestsResp, timeMs int, upsert bool) error

	// GetSelectionOverview retrieves an overview of the selected tests for the corresponding build.
	GetSelectionOverview(ctx context.Context, accountID, orgId, projectId, pipelineId, buildId, stepId, stageId string) (types.SelectionOverview, error)

	//  WriteDiffFiles writes modified files for the build. This information is required
	//  while merging partial call graph.
	WriteDiffFiles(ctx context.Context, accountID, orgId, projectId, pipelineId,
		buildId, stageId, stepId string, diff types.DiffInfo) error

	// GetDiffFiles gets the list of modified files corresponding to a build.
	GetDiffFiles(ctx context.Context, accountID, orgId, projectId, pipelineId,
		buildId, stageId, stepId string) (types.DiffInfo, error)

	// GetReportsInfo returns steps/stages which have reports to show
	GetReportsInfo(ctx context.Context, accountID, orgId, projectId, pipelineId,
		buildId string) ([]types.StepInfo, error)

	// GetIntelligenceInfo returns steps/stages which have test intelligence information to show
	GetIntelligenceInfo(ctx context.Context, accountID, orgId, projectId, pipelineId,
		buildId string) ([]types.StepInfo, error)
}
