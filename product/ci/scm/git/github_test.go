package git

import (
	"context"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
)

// NB make sure there is no existing Branch by this name
func TestCreateBranchGithub(t *testing.T) {
	if os.Getenv("GITHUB_ACCESS_TOKEN") == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.CreateBranchRequest{
		Slug:     "tphoney/scm-test",
		Name:     "test_branch",
		CommitId: "312797ba52425353dec56871a255e2a36fc96344",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: os.Getenv("GITHUB_ACCESS_TOKEN"),
					},
				},
			},
			Debug: true,
		},
	}
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := CreateBranch(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, int32(201), got.Status, "Correct http response")
}

// NB make sure there is no existing PR for this branch, or the test will fail.
func TestCreatePRGithub(t *testing.T) {
	if os.Getenv("GITHUB_ACCESS_TOKEN") == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.CreatePRRequest{
		Slug:   "tphoney/scm-test",
		Body:   "body text",
		Title:  "pr from scm service",
		Source: "patch1",
		Target: "main",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: os.Getenv("GITHUB_ACCESS_TOKEN"),
					},
				},
			},
		},
	}
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := CreatePR(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, int32(201), got.Status, "Correct http response")
}

func TestGetLatestCommitGithub(t *testing.T) {
	if os.Getenv("GITHUB_ACCESS_TOKEN") == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.GetLatestCommitRequest{
		Slug:   "tphoney/scm-test",
		Branch: "main",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: os.Getenv("GITHUB_ACCESS_TOKEN"),
					},
				},
			},
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := GetLatestCommit(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.NotNil(t, got.CommitId, "There is a commit id")
}

func TestListCommitsGithub(t *testing.T) {
	if os.Getenv("GITHUB_ACCESS_TOKEN") == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.ListCommitsRequest{
		Slug: "tphoney/scm-test",
		Type: &pb.ListCommitsRequest_Branch{
			Branch: "main",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: os.Getenv("GITHUB_ACCESS_TOKEN"),
					},
				},
			},
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := ListCommits(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Greater(t, len(got.CommitIds), 1, "more than 1 commit")
	assert.Equal(t, int32(2), got.Pagination.Next, "there is a next page page")
}

func TestListCommitsPage2Github(t *testing.T) {
	if os.Getenv("GITHUB_ACCESS_TOKEN") == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.ListCommitsRequest{
		Slug: "tphoney/scm-test",
		Type: &pb.ListCommitsRequest_Branch{
			Branch: "main",
		},
		Pagination: &pb.PageRequest{
			Page: 2,
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: os.Getenv("GITHUB_ACCESS_TOKEN"),
					},
				},
			},
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := ListCommits(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Greater(t, len(got.CommitIds), 1, "more than 1 commit")
	assert.Equal(t, int32(3), got.Pagination.Next, "there is a next page page")
}

func TestListBranchesGithub(t *testing.T) {
	if os.Getenv("GITHUB_ACCESS_TOKEN") == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.ListBranchesRequest{
		Slug: "tphoney/scm-test",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: os.Getenv("GITHUB_ACCESS_TOKEN"),
					},
				},
			},
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := ListBranches(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.GreaterOrEqual(t, len(got.Branches), 1, "status matches")
	assert.Equal(t, int32(0), got.Pagination.Next, "there is no next page")
}
