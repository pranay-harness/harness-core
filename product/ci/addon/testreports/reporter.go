package testreports

import (
	"context"
	"github.com/wings-software/portal/product/ci/ti-service/types"
)

// TestReporter is any interface which can send us tests in our custom format
type TestReporter interface {

	// Get test case information
	GetTests(context.Context) (<-chan *types.TestCase, <-chan error)
}
