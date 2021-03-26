package git

import (
	"context"
	"time"

	"github.com/drone/go-scm/scm"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/scm/gitclient"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
)

func GetLatestCommit(ctx context.Context, request *pb.GetLatestCommitRequest, log *zap.SugaredLogger) (out *pb.GetLatestCommitResponse, err error) {
	start := time.Now()
	log.Infow("GetLatestCommit starting", "slug", request.GetSlug())

	client, err := gitclient.GetGitClient(*request.GetProvider(), log)
	if err != nil {
		log.Errorw("GetLatestCommit failure", "bad provider", *request.GetProvider(), "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	ref, err := gitclient.GetValidRef("", request.GetBranch())
	if err != nil {
		log.Errorw("GetLatestCommit failure, bad ref/branch", "provider", *request.GetProvider(), "slug", request.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	response, _, err := client.Git.FindCommit(ctx, request.GetSlug(), ref)
	if err != nil {
		log.Errorw("GetLatestCommit failure", "provider", *request.GetProvider(), "slug", request.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("GetLatestCommit success", "slug", request.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start))

	out = &pb.GetLatestCommitResponse{
		CommitId: response.Sha,
	}
	return out, nil
}

func ListBranches(ctx context.Context, request *pb.ListBranchesRequest, log *zap.SugaredLogger) (out *pb.ListBranchesResponse, err error) {
	start := time.Now()
	log.Infow("ListBranches starting", "slug", request.GetSlug())

	client, err := gitclient.GetGitClient(*request.GetProvider(), log)
	if err != nil {
		log.Errorw("ListBranches failure", "bad provider", *request.GetProvider(), "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	response, _, err := client.Git.ListBranches(ctx, request.GetSlug(), scm.ListOptions{})
	if err != nil {
		log.Errorw("ListBranches failure", "provider", *request.GetProvider(), "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("ListBranches success", "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start))
	var branches []string
	for _, v := range response {
		branches = append(branches, v.Name)
	}

	out = &pb.ListBranchesResponse{
		Branches: branches,
	}
	return out, nil
}

func ListCommits(ctx context.Context, request *pb.ListCommitsRequest, log *zap.SugaredLogger) (out *pb.ListCommitsResponse, err error) {
	start := time.Now()
	log.Infow("ListCommits starting", "slug", request.GetSlug())

	client, err := gitclient.GetGitClient(*request.GetProvider(), log)
	if err != nil {
		log.Errorw("ListCommits failure", "bad provider", *request.GetProvider(), "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	ref, err := gitclient.GetValidRef(request.GetRef(), request.GetBranch())
	if err != nil {
		log.Errorw("ListCommits failure, bad ref/branch", "provider", *request.GetProvider(), "slug", request.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	response, _, err := client.Git.ListCommits(ctx, request.GetSlug(), scm.CommitListOptions{Ref: ref})
	if err != nil {
		log.Errorw("ListCommits failure", "provider", *request.GetProvider(), "slug", request.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("ListCommits success", "slug", request.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start))
	var commit_ids []string
	for _, v := range response {
		commit_ids = append(commit_ids, v.Sha)
	}

	out = &pb.ListCommitsResponse{
		CommitIds: commit_ids,
	}
	return out, nil
}
