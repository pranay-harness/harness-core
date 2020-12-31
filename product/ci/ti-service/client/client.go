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
	Write(ctx context.Context, org, project, pipeline, build, stage, step, report string, tests []*types.TestCase) error
}
