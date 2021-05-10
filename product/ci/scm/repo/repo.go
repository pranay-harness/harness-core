package repo

import (
	"context"
	"fmt"
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

	events, _ := convertStringsToEvents(hook.Events, *request.GetProvider())
	out = &pb.CreateWebhookResponse{
		Webhook: &pb.WebhookResponse{
			Id:         hook.ID,
			Name:       hook.Name,
			Target:     hook.Target,
			Events:     &events,
			Active:     hook.Active,
			SkipVerify: hook.SkipVerify,
		},
		Status: int32(response.Status),
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
		events, _ := convertStringsToEvents(h.Events, *request.GetProvider())
		webhookResponse := pb.WebhookResponse{
			Id:         h.ID,
			Name:       h.Name,
			Target:     h.Target,
			Events:     &events,
			Active:     h.Active,
			SkipVerify: h.SkipVerify,
		}
		hooks = append(hooks, &webhookResponse)
	}

	out = &pb.ListWebhooksResponse{
		Webhooks: hooks,
		Status:   int32(response.Status),
		Pagination: &pb.PageResponse{
			Next: int32(response.Page.Next),
		},
	}
	return out, nil
}

func convertStringsToEvents(strings []string, p pb.Provider) (events pb.HookEvents, err error) {
	set := make(map[string]bool)
	for _, v := range strings {
		set[v] = true
	}
	switch p.GetHook().(type) {
	case *pb.Provider_Github:
		if set["push"] {
			events.Push = true
		}
		if set["pull_request"] {
			events.PullRequest = true
		}
		if set["pull_request_review_comment"] {
			events.PullRequestComment = true
		}
		if set["issue"] {
			events.Issue = true
		}
		if set["issue_comment"] {
			// because this is a 1 to many abstraction it leaks and will cause issues
			events.IssueComment = true
			events.PullRequestComment = true
		}
		if set["create"] || set["delete"] {
			// because this is a 1 to many abstraction it leaks and will cause issues
			events.Branch = true
			events.Tag = true
		}
		if set["deployment"] {
			events.Deployment = true
		}
		return events, nil
	default:
		return events, fmt.Errorf("there is no logic to convertStringsToEvents, for this provider %s", p.GetEndpoint())
	}
}
