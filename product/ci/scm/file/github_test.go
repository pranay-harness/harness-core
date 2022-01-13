// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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

var fileGithubToken = os.Getenv("GITHUB_ACCESS_TOKEN")

func TestFindFileGithub(t *testing.T) {
	if fileGithubToken == "" {
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
						AccessToken: fileGithubToken,
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
	assert.NotEqual(t, got.BlobId, "", "there is a blob_id")
	assert.Equal(t, got.CommitId, "", "there is not a commit_id")
}

func TestCreateUpdateDeleteFileGithub(t *testing.T) {
	if fileGithubToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.FileModifyRequest{
		Slug:    "tphoney/scm-test",
		Path:    "CRUD",
		Content: "hello",
		Branch:  "main",
		Message: "create CRUD",
		Signature: &pb.Signature{
			Name:  "tp honey",
			Email: "tp@harness.io",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: fileGithubToken,
					},
				},
			},
			Debug: true,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	create, err1 := CreateFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err1, "no errors")
	assert.Equal(t, int32(201), create.Status, "status matches")
	assert.Equal(t, "b6fc4c620b67d95f953a5c1c1230aaab5db5a1b0", create.BlobId, "blob id matches")
	assert.NotEqual(t, create.CommitId, "", "there is a commit_id")

	in2 := &pb.FileModifyRequest{
		Slug:    "tphoney/scm-test",
		Path:    "CRUD",
		Content: "hello2",
		Branch:  "main",
		Message: "update CRUD",
		BlobId:  "b6fc4c620b67d95f953a5c1c1230aaab5db5a1b0",
		Signature: &pb.Signature{
			Name:  "tp honey",
			Email: "tp@harness.io",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: fileGithubToken,
					},
				},
			},
			Debug: true,
		},
	}

	log, _ = logs.GetObservedLogger(zap.InfoLevel)
	update, err2 := UpdateFile(context.Background(), in2, log.Sugar())

	assert.Nil(t, err2, "no errors")
	assert.Equal(t, int32(200), update.Status, "status matches")
	assert.Equal(t, "23294b0610492cf55c1c4835216f20d376a287dd", update.BlobId, "blob id matches")
	assert.NotEqual(t, update.CommitId, "", "there is a commit_id")

	in3 := &pb.DeleteFileRequest{
		Slug:    "tphoney/scm-test",
		Path:    "CRUD",
		Branch:  "main",
		Message: "delete CRUD",
		BlobId:  "23294b0610492cf55c1c4835216f20d376a287dd",
		Signature: &pb.Signature{
			Name:  "tp honey",
			Email: "tp@harness.io",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: fileGithubToken,
					},
				},
			},
			Debug: true,
		},
	}

	log, _ = logs.GetObservedLogger(zap.InfoLevel)
	del, err3 := DeleteFile(context.Background(), in3, log.Sugar())

	assert.Nil(t, err3, "no errors")
	assert.Equal(t, int32(200), del.Status, "status matches")
	assert.NotEqual(t, "", del.CommitId, "commit id is not ''")
	assert.Equal(t, "", del.BlobId, "blob id is ''")
}

func TestFindFilesInCommitGithub(t *testing.T) {
	if fileGithubToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.FindFilesInCommitRequest{
		Slug: "tphoney/scm-test",
		Ref:  "9a9b31a127e7ed3ee781b6268ae3f9fb7e4525bb",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: fileGithubToken,
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
	if fileGithubToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.FindFilesInBranchRequest{
		Slug: "tphoney/scm-test",
		Type: &pb.FindFilesInBranchRequest_Branch{
			Branch: "main",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: fileGithubToken,
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
	if fileGithubToken == "" {
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
						AccessToken: fileGithubToken,
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
						AccessToken: fileGithubToken,
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
