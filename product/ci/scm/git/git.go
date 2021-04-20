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

func CreatePR(ctx context.Context, request *pb.CreatePRRequest, log *zap.SugaredLogger) (out *pb.CreatePRResponse, err error) {
	start := time.Now()
	log.Infow("CreatePR starting", "slug", request.GetSlug())

	client, err := gitclient.GetGitClient(*request.GetProvider(), log)
	if err != nil {
		log.Errorw("CreatePR failure", "bad provider", request.GetProvider(), "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	inputParams := scm.PullRequestInput{
		Title:  request.Title,
		Body:   request.Body,
		Target: request.Target,
		Source: request.Source,
	}

	_, response, err := client.PullRequests.Create(ctx, request.GetSlug(), &inputParams)
	if err != nil {
		log.Errorw("CreatePR failure", "provider", request.GetProvider(), "slug", request.GetSlug(), "source", request.GetSource(), "target", request.GetTarget(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("CreatePR success", "slug", request.GetSlug(), "source", request.GetSource(), "target", request.GetTarget(), "elapsed_time_ms", utils.TimeSince(start))

	out = &pb.CreatePRResponse{
		Status: int32(response.Status),
	}
	return out, nil
}

func CreateBranch(ctx context.Context, request *pb.CreateBranchRequest, log *zap.SugaredLogger) (out *pb.CreateBranchResponse, err error) {
	start := time.Now()
	log.Infow("CreateBranch starting", "slug", request.GetSlug())

	client, err := gitclient.GetGitClient(*request.GetProvider(), log)
	if err != nil {
		log.Errorw("CreateBranch failure", "bad provider", request.GetProvider(), "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	inputParams := scm.CreateBranch{
		Name: request.GetName(),
		Sha:  request.GetCommitId(),
	}

	response, err := client.Git.CreateBranch(ctx, request.GetSlug(), &inputParams)
	if err != nil {
		log.Errorw("CreateBranch failure", "provider", request.GetProvider(), "slug", request.GetSlug(), "Name", request.GetName(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("CreateBranch success", "slug", request.GetSlug(), "Name", request.GetName(), "elapsed_time_ms", utils.TimeSince(start))

	out = &pb.CreateBranchResponse{
		Status: int32(response.Status),
	}
	return out, nil
}

func GetLatestCommit(ctx context.Context, request *pb.GetLatestCommitRequest, log *zap.SugaredLogger) (out *pb.GetLatestCommitResponse, err error) {
	start := time.Now()
	log.Infow("GetLatestCommit starting", "slug", request.GetSlug())

	client, err := gitclient.GetGitClient(*request.GetProvider(), log)
	if err != nil {
		log.Errorw("GetLatestCommit failure", "bad provider", request.GetProvider(), "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	ref, err := gitclient.GetValidRef(*request.Provider, "", request.GetBranch())
	if err != nil {
		log.Errorw("GetLatestCommit failure, bad ref/branch", "provider", request.GetProvider(), "slug", request.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	response, _, err := client.Git.FindCommit(ctx, request.GetSlug(), ref)
	if err != nil {
		log.Errorw("GetLatestCommit failure", "provider", request.GetProvider(), "slug", request.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
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
		log.Errorw("ListBranches failure", "bad provider", request.GetProvider(), "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	branchesContent, response, err := client.Git.ListBranches(ctx, request.GetSlug(), scm.ListOptions{Page: int(request.GetPagination().GetPage())})
	if err != nil {
		log.Errorw("ListBranches failure", "provider", request.GetProvider(), "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("ListBranches success", "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start))
	var branches []string
	for _, v := range branchesContent {
		branches = append(branches, v.Name)
	}

	out = &pb.ListBranchesResponse{
		Branches: branches,
		Pagination: &pb.PageResponse{
			Next: int32(response.Page.Next),
		},
	}
	return out, nil
}

func ListCommits(ctx context.Context, request *pb.ListCommitsRequest, log *zap.SugaredLogger) (out *pb.ListCommitsResponse, err error) {
	start := time.Now()
	log.Infow("ListCommits starting", "slug", request.GetSlug())

	client, err := gitclient.GetGitClient(*request.GetProvider(), log)
	if err != nil {
		log.Errorw("ListCommits failure", "bad provider", request.GetProvider(), "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	ref, err := gitclient.GetValidRef(*request.Provider, request.GetRef(), request.GetBranch())
	if err != nil {
		log.Errorw("ListCommits failure, bad ref/branch", "provider", request.GetProvider(), "slug", request.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	commits, response, err := client.Git.ListCommits(ctx, request.GetSlug(), scm.CommitListOptions{Ref: ref, Page: int(request.GetPagination().GetPage())})
	if err != nil {
		log.Errorw("ListCommits failure", "provider", request.GetProvider(), "slug", request.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("ListCommits success", "slug", request.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start))
	var commit_ids []string
	for _, v := range commits {
		commit_ids = append(commit_ids, v.Sha)
	}

	out = &pb.ListCommitsResponse{
		CommitIds: commit_ids,
		Pagination: &pb.PageResponse{
			Next: int32(response.Page.Next),
		},
	}
	return out, nil
}
