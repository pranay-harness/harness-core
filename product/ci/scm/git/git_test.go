package git

import (
	"context"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
)

func TestCreatePR(t *testing.T) {
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

func TestFindFilesInPR(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		content, _ := ioutil.ReadFile("testdata/pr_files.json")
		fmt.Fprint(w, string(content))
	}))
	defer ts.Close()
	in := &pb.FindFilesInPRRequest{
		Slug:   "tphoney/scm-test",
		Number: int32(102),
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
	got, err := FindFilesInPR(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, 1, len(got.Files), "1 file")
	assert.Equal(t, int32(0), got.Pagination.Next, "No next page")
}

func TestCreateBranch(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(201)
		content, _ := ioutil.ReadFile("testdata/branch.json")
		fmt.Fprint(w, string(content))
	}))
	defer ts.Close()

	in := &pb.CreateBranchRequest{
		Slug:     "tphoney/scm-test",
		Name:     "yooo",
		CommitId: "0efb1bed7c6a4871cb4ddb862ecc2111e11f31ee",
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
	got, err := CreateBranch(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, int32(201), got.Status, "Correct http response")
}

func TestGetLatestCommit(t *testing.T) {
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

func TestListBranches(t *testing.T) {
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
	assert.Equal(t, 1, len(got.Branches), "one branch")
	assert.Equal(t, int32(0), got.Pagination.Next, "No next page")
}

func TestListCommits(t *testing.T) {
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
	assert.Equal(t, int32(0), got.Pagination.Next, "No next page")
}

func TestListCommitsInPR(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		content, _ := ioutil.ReadFile("testdata/commits.json")
		fmt.Fprint(w, string(content))
	}))
	defer ts.Close()

	in := &pb.ListCommitsInPRRequest{
		Slug:   "tphoney/scm-test",
		Number: 1234,
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
	got, err := ListCommitsInPR(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, len(got.Commits), 1, "1 commit")
	assert.Equal(t, int32(0), got.Pagination.Next, "No next page")
}
