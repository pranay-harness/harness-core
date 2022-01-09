// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
