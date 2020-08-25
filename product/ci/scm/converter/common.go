package converter

import (
	"github.com/drone/go-scm/scm"
	"github.com/golang/protobuf/ptypes"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
)

// convertAction converts scm.Action to protobuf object
func convertAction(a scm.Action) pb.Action {
	switch a {
	case scm.ActionCreate:
		return pb.Action_CREATE
	case scm.ActionUpdate:
		return pb.Action_UPDATE
	case scm.ActionDelete:
		return pb.Action_DELETE
	case scm.ActionOpen:
		return pb.Action_OPEN
	case scm.ActionReopen:
		return pb.Action_REOPEN
	case scm.ActionClose:
		return pb.Action_CLOSE
	case scm.ActionLabel:
		return pb.Action_LABEL
	case scm.ActionUnlabel:
		return pb.Action_UNLABEL
	case scm.ActionSync:
		return pb.Action_SYNC
	case scm.ActionMerge:
		return pb.Action_MERGE
	default:
		return pb.Action_UNKNOWN
	}
}

// convertUser converts scm.User to protobuf object
func convertUser(u scm.User) (*pb.User, error) {
	createTs, err := ptypes.TimestampProto(u.Created)
	if err != nil {
		return nil, err
	}
	updateTs, err := ptypes.TimestampProto(u.Updated)
	if err != nil {
		return nil, err
	}
	return &pb.User{
		Login:   u.Login,
		Name:    u.Name,
		Email:   u.Email,
		Avatar:  u.Avatar,
		Created: createTs,
		Updated: updateTs,
	}, nil
}

// convertReference converts scm.Reference to protobuf object
func convertReference(r scm.Reference) *pb.Reference {
	return &pb.Reference{
		Name: r.Name,
		Path: r.Path,
		Sha:  r.Sha,
	}
}

// convertPerm converts *scm.Perm to protobuf object
func convertPerm(p *scm.Perm) *pb.Perm {
	if p == nil {
		return nil
	}
	return &pb.Perm{
		Pull:  p.Pull,
		Push:  p.Push,
		Admin: p.Admin,
	}
}

// convertRepo converts scm.Repository to protobuf object
func convertRepo(r scm.Repository) (*pb.Repository, error) {
	createTs, err := ptypes.TimestampProto(r.Created)
	if err != nil {
		return nil, err
	}
	updateTs, err := ptypes.TimestampProto(r.Updated)
	if err != nil {
		return nil, err
	}

	return &pb.Repository{
		Id:        r.ID,
		Namespace: r.Namespace,
		Name:      r.Name,
		Perm:      convertPerm(r.Perm),
		Branch:    r.Branch,
		Private:   r.Private,
		Clone:     r.Clone,
		CloneSsh:  r.CloneSSH,
		Link:      r.Link,
		Created:   createTs,
		Updated:   updateTs,
	}, nil
}
