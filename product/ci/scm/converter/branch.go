// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package converter

import (
	"github.com/drone/go-scm/scm"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
)

// ConvertBranchHook converts scm.PullRequestHook to protobuf object
func ConvertBranchHook(h *scm.BranchHook) (*pb.BranchHook, error) {
	if h == nil {
		return nil, nil
	}

	repo, err := ConvertRepo(&h.Repo)
	if err != nil {
		return nil, err
	}
	sender, err := convertUser(&h.Sender)
	if err != nil {
		return nil, err
	}

	return &pb.BranchHook{
		Action: convertAction(h.Action),
		Repo:   repo,
		Sender: sender,
		Ref:    convertReference(h.Ref),
	}, nil

}
