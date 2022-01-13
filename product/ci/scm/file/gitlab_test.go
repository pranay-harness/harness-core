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

var fileGitlabToken = os.Getenv("GITLAB_ACCESS_TOKEN")

func TestCreateReadUpdateDeleteFileGitlab(t *testing.T) {
	if fileGitlabToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.FileModifyRequest{
		Slug:    "tphoney/test_repo",
		Path:    "CRUD",
		Content: "hello",
		Message: "create CRUD",
		Branch:  "master",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: fileGitlabToken,
					},
				},
			},
			Debug: true,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	create, err := CreateFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, int32(201), create.Status, "status matches")

	in2 := &pb.GetFileRequest{
		Slug: "tphoney/test_repo",
		Path: "CRUD",
		Type: &pb.GetFileRequest_Branch{
			Branch: "master",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: fileGitlabToken,
					},
				},
			},
			Debug: true,
		},
	}

	log, _ = logs.GetObservedLogger(zap.InfoLevel)
	read, err := FindFile(context.Background(), in2, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Contains(t, read.Content, "hello")

	in3 := &pb.FileModifyRequest{
		Slug:     "tphoney/test_repo",
		Path:     "CRUD",
		Content:  "hello thisiad sad as a 2\nasdasd",
		Branch:   "master",
		Message:  "update CRUD",
		CommitId: read.CommitId,
		Signature: &pb.Signature{
			Name:  "tp honey",
			Email: "tp@harness.io",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: fileGitlabToken,
					},
				},
			},
			Debug: true,
		},
	}

	log, _ = logs.GetObservedLogger(zap.InfoLevel)
	update, err2 := UpdateFile(context.Background(), in3, log.Sugar())

	assert.Nil(t, err2, "no errors")
	assert.Equal(t, int32(200), update.Status, "status matches")

	in4 := &pb.DeleteFileRequest{
		Slug:     "tphoney/test_repo",
		Path:     "CRUD",
		Branch:   "master",
		Message:  "delete CRUD",
		CommitId: update.CommitId,
		Signature: &pb.Signature{
			Name:  "tp honey",
			Email: "tp@harness.io",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: fileGitlabToken,
					},
				},
			},
			Debug: true,
		},
	}

	log, _ = logs.GetObservedLogger(zap.InfoLevel)
	del, err3 := DeleteFile(context.Background(), in4, log.Sugar())
	assert.Nil(t, err3, "no errors")
	assert.Equal(t, int32(204), del.Status, "status matches")
}

func TestFindFilesInCommitGitlab(t *testing.T) {
	if fileGitlabToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.FindFilesInCommitRequest{
		Slug: "tphoney/test_repo",
		Ref:  "b362ea7aa65515dc35ff3a93423478b2143e771d",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: fileGitlabToken,
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

func TestFindFilesInBranchGitlab(t *testing.T) {
	if fileGitlabToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.FindFilesInBranchRequest{
		Slug: "tphoney/test_repo",
		Type: &pb.FindFilesInBranchRequest_Branch{
			Branch: "master",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Gitlab{
				Gitlab: &pb.GitlabProvider{
					Provider: &pb.GitlabProvider_AccessToken{
						AccessToken: fileGitlabToken,
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
