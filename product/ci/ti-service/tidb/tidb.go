// package tidb defines a DB interface for test intelligence DB
package tidb

import (
	"context"
	"github.com/wings-software/portal/product/ci/addon/ti"
	"github.com/wings-software/portal/product/ci/ti-service/tidb/mongodb"
	"github.com/wings-software/portal/product/ci/ti-service/types"
)

type TiDB interface {
	// GetTestsToRun reads in the changed files and returns which tests to run.
	GetTestsToRun(ctx context.Context, req types.SelectTestsReq) (types.SelectTestsResp, error)

	// GetTestsToRun uploads partial cg corresponding to a branch to mongo
	UploadPartialCg(ctx context.Context, cg *ti.Callgraph, info mongodb.VCSInfo, acc, org, proj string) error
}
