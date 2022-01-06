// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package converter

import (
	"github.com/drone/go-scm/scm"
	"github.com/golang/protobuf/ptypes"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
)

// ConvertIssueCommentHook converts scm.IssueCommentHook to protobuf object
func ConvertIssueCommentHook(h *scm.IssueCommentHook) (*pb.IssueCommentHook, error) {
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

	issue, err := convertIssue(&h.Issue)
	if err != nil {
		return nil, err
	}

	comment, err := convertComment(&h.Comment)
	if err != nil {
		return nil, err
	}

	return &pb.IssueCommentHook{
		Action:  convertAction(h.Action),
		Repo:    repo,
		Comment: comment,
		Issue:   issue,
		Sender:  sender,
	}, nil
}

// convertComment converts scm.comment to protobuf object
func convertComment(c *scm.Comment) (*pb.Comment, error) {
	user, err := convertUser(&c.Author)
	if err != nil {
		return nil, err
	}

	createTS, err := ptypes.TimestampProto(c.Created)
	if err != nil {
		return nil, err
	}

	updateTS, err := ptypes.TimestampProto(c.Updated)
	if err != nil {
		return nil, err
	}

	return &pb.Comment{
		Id:      int32(c.ID),
		Body:    c.Body,
		User:    user,
		Created: createTS,
		Updated: updateTS,
	}, nil
}

// convertIssue converts scm.issue to protobuf object
func convertIssue(i *scm.Issue) (*pb.Issue, error) {
	user, err := convertUser(&i.Author)
	if err != nil {
		return nil, err
	}

	createTS, err := ptypes.TimestampProto(i.Created)
	if err != nil {
		return nil, err
	}

	updateTS, err := ptypes.TimestampProto(i.Updated)
	if err != nil {
		return nil, err
	}

	pr, err := ConvertPR(&i.PullRequest)
	if err != nil {
		return nil, err
	}

	var labels []string

	labels = append(labels, i.Labels...)

	return &pb.Issue{
		Number:  int32(i.Number),
		Title:   i.Title,
		Body:    i.Body,
		Link:    i.Link,
		Labels:  labels,
		Closed:  i.Closed,
		Locked:  i.Locked,
		User:    user,
		Created: createTS,
		Updated: updateTS,
		Pr:      pr,
	}, nil
}
