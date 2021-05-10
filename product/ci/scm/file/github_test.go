package file

import (
	"context"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
)

func TestFindFileGithub(t *testing.T) {
	if os.Getenv("GITHUB_ACCESS_TOKEN") == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.GetFileRequest{
		Slug: "tphoney/scm-test",
		Path: "README.md",
		Type: &pb.GetFileRequest_Ref{
			Ref: "main",
		},
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
	got, err := FindFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Contains(t, got.Content, "test repo for source control operations")
}

func TestUpdateFileGithub(t *testing.T) {
	if os.Getenv("GITHUB_ACCESS_TOKEN") == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.FileModifyRequest{
		Slug:    "tphoney/scm-test",
		Path:    "jello",
		Content: "hello",
		Branch:  "main",
		BlobId:  "b6fc4c620b67d95f953a5c1c1230aaab5db5a1b0",
		Signature: &pb.Signature{
			Name:  "tp honey",
			Email: "tp@harness.io",
		},
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
	got, err := UpdateFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, int32(200), got.Status, "status matches")
}

func TestFindFilesInCommitGithub(t *testing.T) {
	if os.Getenv("GITHUB_ACCESS_TOKEN") == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.FindFilesInCommitRequest{
		Slug: "tphoney/scm-test",
		Ref:  "9a9b31a127e7ed3ee781b6268ae3f9fb7e4525bb",
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
	got, err := FindFilesInCommit(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, 1, len(got.File), "1 file")
}

func TestFindFilesInBranchGithub(t *testing.T) {
	if os.Getenv("GITHUB_ACCESS_TOKEN") == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.FindFilesInBranchRequest{
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
			Debug: true,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := FindFilesInBranch(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.LessOrEqual(t, 2, len(got.File), "More than 2 files in branch")
}

func TestBatchFindFilesGithub(t *testing.T) {
	if os.Getenv("GITHUB_ACCESS_TOKEN") == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in1 := &pb.GetFileRequest{
		Slug: "tphoney/scm-test",
		Path: "README.md",
		Type: &pb.GetFileRequest_Ref{
			Ref: "main",
		},
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

	in2 := &pb.GetFileRequest{
		Slug: "tphoney/scm-test",
		Path: "NOTHING",
		Type: &pb.GetFileRequest_Ref{
			Ref: "main",
		},
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
	in := &pb.GetBatchFileRequest{
		FindRequest: []*pb.GetFileRequest{in1, in2},
	}
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := BatchFindFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Contains(t, got.FileContents[0].Content, "test repo for source control operations")
	assert.Equal(t, "", got.FileContents[1].Content, "missing file has no content")
	assert.Equal(t, "Not Found", got.FileContents[1].Error, "missing file has error of 'Not Found'")
}
