package client

import (
	"context"
	"github.com/wings-software/portal/product/ci/ti-service/types"
)

// Error represents a json-encoded API error.
type Error struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
}

func (e *Error) Error() string {
	return e.Message
}

// Client defines a TI service client.
type Client interface {
	// Write test cases to DB
	Write(ctx context.Context, org, project, pipeline, build, stage, step, report, repo, sha string, tests []*types.TestCase) error

	// SelectTests returns list of tests which should be run intelligently
	SelectTests(org, project, pipeline, build, stage, step, repo, sha, source, target, req string) (types.SelectTestsResp, error)

	// UploadCg uploads avro encoded callgraph to ti server
	UploadCg(org, project, pipeline, build, stage, step, repo, sha, source, target string, timeMs int64, cg []byte) error
}
