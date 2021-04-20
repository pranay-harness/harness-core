package converter

import (
	"github.com/drone/go-scm/scm"
	"github.com/golang/protobuf/ptypes"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
)

// ConvertPRHook converts scm.PullRequestHook to protobuf object
func ConvertPRHook(h *scm.PullRequestHook) (*pb.PullRequestHook, error) {
	if h == nil {
		return nil, nil
	}

	repo, err := convertRepo(&h.Repo)
	if err != nil {
		return nil, err
	}
	pr, err := convertPR(&h.PullRequest)
	if err != nil {
		return nil, err
	}
	sender, err := convertUser(&h.Sender)
	if err != nil {
		return nil, err
	}

	return &pb.PullRequestHook{
		Action: convertAction(h.Action),
		Repo:   repo,
		Pr:     pr,
		Sender: sender,
	}, nil
}

// convertPR converts scm.PullRequest to protobuf object
func convertPR(pr *scm.PullRequest) (*pb.PullRequest, error) {
	author, err := convertUser(&pr.Author)
	if err != nil {
		return nil, err
	}
	createTS, err := ptypes.TimestampProto(pr.Created)
	if err != nil {
		return nil, err
	}
	updateTS, err := ptypes.TimestampProto(pr.Updated)
	if err != nil {
		return nil, err
	}

	var labels []*pb.Label
	for _, l := range pr.Labels {
		labels = append(labels, convertLabel(l))
	}
	return &pb.PullRequest{
		Number:  int64(pr.Number),
		Title:   pr.Title,
		Body:    pr.Body,
		Sha:     pr.Sha,
		Ref:     pr.Ref,
		Source:  pr.Source,
		Target:  pr.Target,
		Fork:    pr.Fork,
		Link:    pr.Link,
		Closed:  pr.Closed,
		Merged:  pr.Merged,
		Base:    convertReference(pr.Base),
		Head:    convertReference(pr.Head),
		Author:  author,
		Created: createTS,
		Updated: updateTS,
		Labels:  labels,
	}, nil
}
