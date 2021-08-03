// package tidb defines a DB interface for test intelligence DB
package tidb

import (
	"context"
	cgp "github.com/wings-software/portal/product/ci/addon/parser/cg"
	"github.com/wings-software/portal/product/ci/ti-service/tidb/mongodb"
	"github.com/wings-software/portal/product/ci/ti-service/types"
)

type TiDB interface {
	// GetTestsToRun reads in the changed files and returns which tests to run.
	GetTestsToRun(ctx context.Context, req types.SelectTestsReq, account string, enableReflection bool) (types.SelectTestsResp, error)

	// UploadPartialCg uploads a call graph
	UploadPartialCg(ctx context.Context, cg *cgp.Callgraph, info mongodb.VCSInfo, account, org, proj, target string) (types.SelectTestsResp, error)

	// MergePartialCg merges a partial cg corresponding to a list of commits and a repo to the
	// master call graph.
	MergePartialCg(ctx context.Context, req types.MergePartialCgRequest) error
}
