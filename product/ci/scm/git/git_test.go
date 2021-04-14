package git

import (
	"context"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
)

func TestCreatePRGithub(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(201)
		content, _ := ioutil.ReadFile("testdata/pr.json")
		fmt.Fprint(w, string(content))
	}))
	defer ts.Close()

	in := &pb.CreatePRRequest{
		Slug:   "tphoney/scm-test",
		Body:   "body text",
		Title:  "pr from scm service",
		Source: "branchy",
		Target: "main",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
					},
				},
			},
			Endpoint: ts.URL,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := CreatePR(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, int32(201), got.Status, "Correct http response")
}

func TestGetLatestCommitGithub(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		content, _ := ioutil.ReadFile("testdata/commit.json")
		fmt.Fprint(w, string(content))
	}))
	defer ts.Close()

	in := &pb.GetLatestCommitRequest{
		Slug:   "tphoney/scm-test",
		Branch: "main",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
					},
				},
			},
			Endpoint: ts.URL,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := GetLatestCommit(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.NotNil(t, got.CommitId, "There is a commit id")
}

func TestListBranchesGithub(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		content, _ := ioutil.ReadFile("testdata/branches.json")
		fmt.Fprint(w, string(content))
	}))
	defer ts.Close()

	in := &pb.ListBranchesRequest{
		Slug: "tphoney/scm-test",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
					},
				},
			},
			Endpoint: ts.URL,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := ListBranches(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, len(got.Branches), 1, "one branch")
}

func TestListCommitsGithub(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		content, _ := ioutil.ReadFile("testdata/commits.json")
		fmt.Fprint(w, string(content))
	}))
	defer ts.Close()

	in := &pb.ListCommitsRequest{
		Slug: "tphoney/scm-test",
		Type: &pb.ListCommitsRequest_Branch{
			Branch: "main",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
					},
				},
			},
			Endpoint: ts.URL,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := ListCommits(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, len(got.CommitIds), 1, "1 commit")
}
func TestListCommitsGithubRealRequest(t *testing.T) {
	in := &pb.ListCommitsRequest{
		Slug: "tphoney/scm-test",
		Type: &pb.ListCommitsRequest_Branch{
			Branch: "main",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
					},
				},
			},
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := ListCommits(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Greater(t, len(got.CommitIds), 1, "more than 1 commit")
}

// NB make sure there is no existing PR for this branch, or the test will fail.
func TestCreatePRGithubRealRequest(t *testing.T) {
	if os.Getenv("GITHUB_ACCESS_TOKEN") == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.CreatePRRequest{
		Slug:   "tphoney/scm-test",
		Body:   "body text",
		Title:  "pr from scm service",
		Source: "branchy",
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

func TestGetLatestCommitGithubRealRequest(t *testing.T) {
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

func TestListBranchesGithubRealRequest(t *testing.T) {
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
}
