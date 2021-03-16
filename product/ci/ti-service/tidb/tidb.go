// package tidb defines a DB interface for test intelligence DB
package tidb

import (
	"context"
	"github.com/wings-software/portal/product/ci/ti-service/types"
)

type TiDB interface {
	// GetTestsToRun reads in the changed files and returns which tests to run.
	GetTestsToRun(ctx context.Context, files []string) (types.SelectTestsResp, error)
}
