// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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

var gitGitlabToken = os.Getenv("GITLAB_ACCESS_TOKEN")

// NB make sure there is no existing Branch by this name
func TestCreateBranchGitlab(t *testing.T) {
	if gitGitlabToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.CreateBranchRequest{
		Slug:     "tphoney/test_repo",
		Name:     "test_branch",
		CommitId: "e2a43d299c47247e6198d3bdae074817f1534ef3",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: gitGitlabToken,
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

func TestCreatePRGitlab(t *testing.T) {
	if gitGitlabToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.CreatePRRequest{
		Slug:   "tphoney/test_repo",
		Body:   "body text",
		Title:  "pr from scm service",
		Source: "test_branch",
		Target: "main",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: gitGitlabToken,
					},
				},
			},
			Debug: true,
		},
	}
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := CreatePR(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, int32(201), got.Status, "Correct http response")
}

func TestGetLatestCommitGitlab(t *testing.T) {
	if gitGitlabToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.GetLatestCommitRequest{
		Slug: "tphoney/test_repo",
		Type: &pb.GetLatestCommitRequest_Branch{
			Branch: "main",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: gitGitlabToken,
					},
				},
			},
			Debug: true,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := GetLatestCommit(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.NotNil(t, got.Commit.Sha, "There is a commit id")
}

func TestListCommitsGitlab(t *testing.T) {
	if gitGitlabToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.ListCommitsRequest{
		Slug: "tphoney/test_repo",
		Type: &pb.ListCommitsRequest_Branch{
			Branch: "master",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: gitGitlabToken,
					},
				},
			},
			Debug: true,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := ListCommits(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Greater(t, len(got.CommitIds), 1, "more than 1 commit")
	assert.Equal(t, int32(2), got.Pagination.Next, "there is a next page")
}

func TestListCommitsPage2Gitlab(t *testing.T) {
	if gitGitlabToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.ListCommitsRequest{
		Slug: "tphoney/test_repo",
		Type: &pb.ListCommitsRequest_Branch{
			Branch: "master",
		},
		Pagination: &pb.PageRequest{
			Page: 2,
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: gitGitlabToken,
					},
				},
			},
			Debug: true,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := ListCommits(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Greater(t, len(got.CommitIds), 1, "more than 1 commit")
	assert.Equal(t, int32(3), got.Pagination.Next, "there is a next page")
}

func TestListBranchesGitlab(t *testing.T) {
	if gitGitlabToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.ListBranchesRequest{
		Slug: "tphoney/test_repo",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: gitGitlabToken,
					},
				},
			},
			Debug: true,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := ListBranches(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.GreaterOrEqual(t, len(got.Branches), 1, "status matches")
	assert.Equal(t, int32(2), got.Pagination.Next, "there is a next page")
}

func TestCompareCommitsGitlab(t *testing.T) {
	if gitGitlabToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.CompareCommitsRequest{
		Slug:   "tphoney/test_repo",
		Target: "1d96f6e180f445a1b463e9461599b250dc43105c",
		Source: "fd016649d74eb9e49b40379c8558b7f3ee9456f2",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: gitGitlabToken,
					},
				},
			},
			Debug: true,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := CompareCommits(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.GreaterOrEqual(t, len(got.Files), 1, "1 file is different")
	assert.Equal(t, int32(0), got.Pagination.Next, "there is no next page")
}
