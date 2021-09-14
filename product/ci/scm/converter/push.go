// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package converter

import (
	"github.com/drone/go-scm/scm"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
)

// ConvertPushHook converts scm.PushHook to protobuf object
func ConvertPushHook(p *scm.PushHook) (*pb.PushHook, error) {
	repo, err := ConvertRepo(&p.Repo)
	if err != nil {
		return nil, err
	}
	commit, err := ConvertCommit(&p.Commit)
	if err != nil {
		return nil, err
	}
	sender, err := convertUser(&p.Sender)
	if err != nil {
		return nil, err
	}

	var commits []*pb.Commit
	for i := range p.Commits {
		convertedCommit, err := ConvertCommit(&p.Commits[i])
		if err != nil {
			return nil, err
		}
		commits = append(commits, convertedCommit)
	}
	return &pb.PushHook{
		Ref:     p.Ref,
		BaseRef: p.BaseRef,
		Repo:    repo,
		Before:  p.Before,
		After:   p.After,
		Commit:  commit,
		Commits: commits,
		Sender:  sender,
	}, nil
}
