// cgservice package contains functions to interact with callgraph
package cgservice

import (
	"context"
	"github.com/wings-software/portal/product/ci/addon/ti"
	"github.com/wings-software/portal/product/ci/ti-service/types"
)

// CgService represents an interface to run tests intelligently
type CgService interface {

	// GetTestsToRun returns tests to be run given a list of changed files.
	GetTestsToRun(ctx context.Context, req types.SelectTestsReq) (types.SelectTestsResp, error)

	// UploadPartialCg uploads callgraph corresponding to a branch in PR run in mongo.
	UploadPartialCg(ctx context.Context, cg *ti.Callgraph, info VCSInfo, acc, org, proj, target string) (types.SelectTestsResp, error)

	// MergePartialCg merges partial callgraph of from source branch to dest branch in case corresponding pr is merged
	// It also cleans up the nodes which have been deleted in the PR from nodes and relations collections.
	MergePartialCg(ctx context.Context, req types.MergePartialCgRequest) error
}
