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

var fileBitbucketCloudToken = os.Getenv("BITBUCKET_CLOUD_TOKEN")

func TestFindFileBitbucketCloud(t *testing.T) {
	if fileBitbucketCloudToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
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
					AppPassword: fileBitbucketCloudToken,
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

func TestFindThenUpdateFileBitbucketCloud(t *testing.T) {
	if fileBitbucketCloudToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.GetFileRequest{
		Slug: "tphoney/scm-test",
		Path: "jello",
		Type: &pb.GetFileRequest_Ref{
			Ref: "master",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_BitbucketCloud{
				BitbucketCloud: &pb.BitbucketCloudProvider{
					Username:    "tphoney",
					AppPassword: fileBitbucketCloudToken,
				},
			},
			Debug: true,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := FindFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Contains(t, got.Content, "hello")

	fileModifyRequest := &pb.FileModifyRequest{
		Slug:     "tphoney/scm-test",
		Path:     "jello",
		Content:  "hello",
		CommitId: got.CommitId,
		Signature: &pb.Signature{
			Name:  "tp honey",
			Email: "tp@harness.io",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_BitbucketCloud{
				BitbucketCloud: &pb.BitbucketCloudProvider{
					Username:    "tphoney",
					AppPassword: fileBitbucketCloudToken,
				},
			},
			Debug: true,
		},
	}

	log, _ = logs.GetObservedLogger(zap.InfoLevel)
	got2, err2 := UpdateFile(context.Background(), fileModifyRequest, log.Sugar())

	assert.Nil(t, err2, "no errors")
	assert.Equal(t, int32(201), got2.Status, "status matches")
}

func TestFindFilesInCommitBitbucketCloud(t *testing.T) {
	if fileBitbucketCloudToken == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.FindFilesInCommitRequest{
		Slug: "tphoney/scm-test",
		Ref:  "23b6c0b0efe0a9a3ab26a8bbab50e5fdb790d221",
		Provider: &pb.Provider{
			Hook: &pb.Provider_BitbucketCloud{
				BitbucketCloud: &pb.BitbucketCloudProvider{
					Username:    "tphoney",
					AppPassword: fileBitbucketCloudToken,
				},
			},
			Debug: true,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := FindFilesInCommit(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, 8, len(got.File), "8 files")
}

func TestFindFilesInBranchBitbucketCloud(t *testing.T) {
	if os.Getenv("BITBUCKET_CLOUD_TOKEN") == "" {
		t.Skip("Skipping, Acceptance test")
	}
	in := &pb.FindFilesInBranchRequest{
		Slug: "tphoney/scm-test",
		Type: &pb.FindFilesInBranchRequest_Branch{
			Branch: "main",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_BitbucketCloud{
				BitbucketCloud: &pb.BitbucketCloudProvider{
					Username:    "tphoney",
					AppPassword: os.Getenv("BITBUCKET_CLOUD_TOKEN"),
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
