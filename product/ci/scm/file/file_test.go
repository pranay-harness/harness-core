package file

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

func TestFindFilePositivePath(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		content, _ := ioutil.ReadFile("testdata/FileFindSource.json")
		fmt.Fprint(w, string(content))
	}))
	defer ts.Close()

	in := &pb.GetFileRequest{
		Slug: "tphoney/scm-test",
		Path: "jello",
		Type: &pb.GetFileRequest_Branch{
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
	got, err := FindFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Contains(t, got.Content, "test repo for source control operations")
}

func TestFindFileNegativePath(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(404)
		content, _ := ioutil.ReadFile("testdata/FileErrorSource.json")
		fmt.Fprint(w, string(content))
	}))
	defer ts.Close()

	in := &pb.GetFileRequest{
		Slug: "tphoney/scm-test",
		Path: "jello",
		Type: &pb.GetFileRequest_Branch{
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
	got, err := FindFile(context.Background(), in, log.Sugar())

	assert.NotNil(t, err, "error thrown")
	assert.Nil(t, got, "Nothing returned")
}
func TestCreateFile(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(201)
		content, _ := ioutil.ReadFile("testdata/FileCreateSource.json")
		fmt.Fprint(w, string(content))
	}))
	defer ts.Close()

	in := &pb.FileModifyRequest{
		Slug:    "tphoney/scm-test",
		Path:    "jello",
		Message: "message",
		Type: &pb.FileModifyRequest_Branch{
			Branch: "main",
		},
		Content: "data",
		Signature: &pb.Signature{
			Name:  "tp honey",
			Email: "tp@harness.io",
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
	got, err := CreateFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, got.Status, int32(201), "status matches")
}

func TestUpdateFile(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		content, _ := ioutil.ReadFile("testdata/FileUpdateSource.json")
		fmt.Fprint(w, string(content))
	}))
	defer ts.Close()

	in := &pb.FileModifyRequest{
		Slug:    "tphoney/scm-test",
		Path:    "jello",
		Message: "message",
		Type: &pb.FileModifyRequest_Branch{
			Branch: "main",
		},
		Content: "data",
		Sha:     "4ea5e4dd2666245c95ea7d4cd353182ea19934b3",
		Signature: &pb.Signature{
			Name:  "tp honey",
			Email: "tp@harness.io",
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
	got, err := UpdateFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, got.Status, int32(200), "status matches")
}

func TestDeleteFile(t *testing.T) {
	in := &pb.DeleteFileRequest{
		Slug: "tphoney/scm-test",
		Path: "jello",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
					},
				},
			},
			Endpoint: "https://localhost:8081",
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	_, err := DeleteFile(context.Background(), in, log.Sugar())

	assert.NotNil(t, err, "throws an error")
}

func TestPushNewFile(t *testing.T) {
	serveActualFile := false
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		if r.Method == http.MethodGet {
			if serveActualFile {
				// 3. file find
				content, _ := ioutil.ReadFile("testdata/FileFindSource.json")
				fmt.Fprint(w, string(content))
			} else {
				// 1. file does not exist yet
				content, _ := ioutil.ReadFile("testdata/FileError.json")
				fmt.Fprint(w, string(content))
			}
		} else {
			// 2. file is created
			content, _ := ioutil.ReadFile("testdata/FileCreateSource.json")
			serveActualFile = true
			fmt.Fprint(w, string(content))
		}
	}))
	defer ts.Close()
	in := &pb.FileModifyRequest{
		Slug:    "tphoney/scm-test",
		Path:    "jello",
		Message: "message",
		Type: &pb.FileModifyRequest_Branch{
			Branch: "main",
		},
		Content: "data",
		Signature: &pb.Signature{
			Name:  "tp honey",
			Email: "tp@harness.io",
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
	got, err := PushFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, got.Status, int32(200), "status matches")
}

func TestBatchFindFileGithubRealRequest(t *testing.T) {
	in1 := &pb.GetFileRequest{
		Slug: "tphoney/scm-test",
		Path: "README.md",
		Type: &pb.GetFileRequest_Branch{
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

	in := &pb.GetBatchFileRequest{
		FindRequest: []*pb.GetFileRequest{in1},
	}
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := BatchFindFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Contains(t, got.FileContents[0].Content, "test repo for source control operations")
}

func TestFindFilesInBranchGithubRealRequest(t *testing.T) {
	in := &pb.FindFilesInBranchRequest{
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
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	_, err := FindFilesInBranch(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	//assert.GreaterOrEqual(t, len(got.File), 1, "more than one file changed")
}

func TestFindFilesInCommitGithubRealRequest(t *testing.T) {
	in := &pb.FindFilesInCommitRequest{
		Slug: "tphoney/scm-test",
		Ref:  "9a9b31a127e7ed3ee781b6268ae3f9fb7e4525bb",
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
	got, err := FindFilesInCommit(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.GreaterOrEqual(t, len(got.File), 1, "more than one file changed")
}
