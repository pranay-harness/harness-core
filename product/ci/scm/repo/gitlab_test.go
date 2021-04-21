package repo

import (
	"context"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
)

func TestCreateAndDeleteWebhookGitlab(t *testing.T) {
	if os.Getenv("GITLAB_ACCESS_TOKEN") == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.CreateWebhookRequest{
		Slug:   "tphoney/test_repo",
		Name:   "drone",
		Target: "https://example.com",
		Secret: "topsecret",
		Events: &pb.HookEvents{
			PullRequest: true,
		},
		SkipVerify: true,
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: os.Getenv("GITLAB_ACCESS_TOKEN"),
					},
				},
			},
			Debug: true,
		},
	}
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := CreateWebhook(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, int32(201), got.Status, "Correct http response")

	del := &pb.DeleteWebhookRequest{
		Slug: "tphoney/test_repo",
		Id:   got.Id,
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: os.Getenv("GITLAB_ACCESS_TOKEN"),
					},
				},
			},
			Debug: true,
		},
	}
	got2, err2 := DeleteWebhook(context.Background(), del, log.Sugar())

	assert.Nil(t, err2, "no errors")
	assert.Equal(t, int32(204), got2.Status, "Correct http response")
}
