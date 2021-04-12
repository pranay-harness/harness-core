package file

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
)

func TestFindFileBitbucketCloudRealRequest(t *testing.T) {
	in := &pb.GetFileRequest{
		Slug: "tphoney/scm-test",
		Path: "README.md",
		Type: &pb.GetFileRequest_Ref{
			Ref: "master",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_BitbucketCloud{
				BitbucketCloud: &pb.BitbucketCloudProvider{
					Username:    "tphoney",
					AppPassword: "UcNZEUnQp3CjWXHtfeWU",
				},
			},
			Debug: true,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := FindFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Contains(t, got.Content, "test repo for source control operations")
}

func TestFindFileGiteaRealRequest(t *testing.T) {
	//c85c8cd30b5e04cdde9a71b4145571db76d7da03
	in := &pb.GetFileRequest{
		Slug: "tphoney/test-scm",
		Path: "README.md",
		Type: &pb.GetFileRequest_Branch{
			Branch: "master",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitea{
				Gitea: &pb.GiteaProvider{AccessToken: "c85c8cd30b5e04cdde9a71b4145571db76d7da03"},
			},
			Endpoint: "https://gitea.com/",
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := FindFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Contains(t, got.Content, "test repo for source control operations")
}

func TestFindFileGitlabRealRequest(t *testing.T) {
	in := &pb.GetFileRequest{
		Slug: "tphoney/test_repo",
		Path: "README.md",
		Type: &pb.GetFileRequest_Branch{
			Branch: "master",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: "XmC-zz1LCY2jRWZSsRpR",
					},
				},
			},
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := FindFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Contains(t, got.Content, "test repo for source control operations")
}
