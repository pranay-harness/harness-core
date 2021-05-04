package repo

import (
	"context"
	"time"

	"github.com/drone/go-scm/scm"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/scm/gitclient"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
)

func CreateWebhook(ctx context.Context, request *pb.CreateWebhookRequest, log *zap.SugaredLogger) (out *pb.CreateWebhookResponse, err error) {
	start := time.Now()
	log.Infow("CreateWebhook starting", "slug", request.GetSlug())

	client, err := gitclient.GetGitClient(*request.GetProvider(), log)
	if err != nil {
		log.Errorw("CreateWebhook failure", "bad provider", request.GetProvider(), "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	inputParams := scm.HookInput{
		Name:   request.GetName(),
		Target: request.GetTarget(),
		Secret: request.GetSecret(),
		Events: scm.HookEvents{
			Branch:             request.GetEvents().GetBranch(),
			Deployment:         request.GetEvents().GetDeployment(),
			Issue:              request.GetEvents().GetIssue(),
			IssueComment:       request.GetEvents().GetIssueComment(),
			PullRequest:        request.GetEvents().GetPullRequest(),
			PullRequestComment: request.GetEvents().GetPullRequestComment(),
			Push:               request.GetEvents().GetPush(),
			ReviewComment:      request.GetEvents().GetReviewComment(),
			Tag:                request.GetEvents().GetTag(),
		},
		SkipVerify: request.SkipVerify,
	}

	hook, response, err := client.Repositories.CreateHook(ctx, request.GetSlug(), &inputParams)
	if err != nil {
		log.Errorw("CreateWebhook failure", "provider", request.GetProvider(), "slug", request.GetSlug(), "name", request.GetName(), "target", request.GetTarget(),
			"elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("CreateWebhook success", "slug", request.GetSlug(), "name", request.GetName(), "target", request.GetTarget(), "elapsed_time_ms", utils.TimeSince(start))

	out = &pb.CreateWebhookResponse{
		Id:         hook.ID,
		Name:       hook.Name,
		Target:     hook.Target,
		Events:     hook.Events,
		Active:     hook.Active,
		SkipVerify: hook.SkipVerify,
		Status:     int32(response.Status),
	}
	return out, nil
}

func DeleteWebhook(ctx context.Context, request *pb.DeleteWebhookRequest, log *zap.SugaredLogger) (out *pb.DeleteWebhookResponse, err error) {
	start := time.Now()
	log.Infow("DeleteWebhook starting", "slug", request.GetSlug())

	client, err := gitclient.GetGitClient(*request.GetProvider(), log)
	if err != nil {
		log.Errorw("DeleteWebhook failure", "bad provider", request.GetProvider(), "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	response, err := client.Repositories.DeleteHook(ctx, request.GetSlug(), request.GetId())
	if err != nil {
		log.Errorw("DeleteWebhook failure", "provider", request.GetProvider(), "slug", request.GetSlug(), "id", request.GetId(),
			"elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("DeleteWebhook success", "slug", request.GetSlug(), "id", request.GetId(), "elapsed_time_ms", utils.TimeSince(start))

	out = &pb.DeleteWebhookResponse{
		Status: int32(response.Status),
	}
	return out, nil
}

func ListWebhooks(ctx context.Context, request *pb.ListWebhooksRequest, log *zap.SugaredLogger) (out *pb.ListWebhooksResponse, err error) {
	start := time.Now()
	log.Infow("ListWebhooks starting", "slug", request.GetSlug())

	client, err := gitclient.GetGitClient(*request.GetProvider(), log)
	if err != nil {
		log.Errorw("ListWebhooks failure", "bad provider", request.GetProvider(), "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	scmHooks, response, err := client.Repositories.ListHooks(ctx, request.GetSlug(), scm.ListOptions{Page: int(request.GetPagination().GetPage())})
	if err != nil {
		log.Errorw("ListWebhooks failure", "provider", request.GetProvider(), "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("ListWebhooks success", "slug", request.GetSlug(), "elapsed_time_ms", utils.TimeSince(start))
	var hooks []*pb.WebhookResponse
	for _, h := range scmHooks {
		webhookResponse := pb.WebhookResponse{
			Id:         h.ID,
			Name:       h.Name,
			Target:     h.Target,
			Events:     h.Events,
			Active:     h.Active,
			SkipVerify: h.SkipVerify,
		}
		hooks = append(hooks, &webhookResponse)
	}

	out = &pb.ListWebhooksResponse{
		Webhooks: hooks,
		Pagination: &pb.PageResponse{
			Next: int32(response.Page.Next),
		},
	}
	return out, nil
}
