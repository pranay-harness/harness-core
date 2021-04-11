package grpc

import (
	"github.com/wings-software/portal/product/ci/scm/file"
	"github.com/wings-software/portal/product/ci/scm/git"
	"github.com/wings-software/portal/product/ci/scm/parser"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
	"golang.org/x/net/context"
)

// handler is used to implement SCMServer
type handler struct {
	stopCh chan bool
	log    *zap.SugaredLogger
}

// NewSCMHandler returns a GRPC handler that implements pb.SCMServer
func NewSCMHandler(stopCh chan bool, log *zap.SugaredLogger) pb.SCMServer {
	return &handler{stopCh, log}
}

// ParseWebhook is used to parse the webhook
func (h *handler) ParseWebhook(ctx context.Context, in *pb.ParseWebhookRequest) (*pb.ParseWebhookResponse, error) {
	return parser.ParseWebhook(ctx, in, h.log)
}

// Createfile is used to create a file
func (h *handler) CreateFile(ctx context.Context, in *pb.FileModifyRequest) (*pb.CreateFileResponse, error) {
	return file.CreateFile(ctx, in, h.log)
}

// DeleteFile is used to delete a file
func (h *handler) DeleteFile(ctx context.Context, in *pb.DeleteFileRequest) (*pb.DeleteFileResponse, error) {
	return file.DeleteFile(ctx, in, h.log)
}

// GetFile is used to return a file
func (h *handler) GetFile(ctx context.Context, in *pb.GetFileRequest) (*pb.FileContent, error) {
	return file.FindFile(ctx, in, h.log)
}

// GetLatestFile is used to return the latest version of a file
func (h *handler) GetLatestFile(ctx context.Context, in *pb.GetLatestFileRequest) (*pb.FileContent, error) {
	log := h.log
	findFileIn := &pb.GetFileRequest{
		Slug: in.Slug,
		Path: in.Path,
		Type: &pb.GetFileRequest_Branch{
			Branch: in.Branch,
		},
		Provider: in.Provider,
	}
	log.Infow("GetLatestFile using FindFile", "slug", in.GetSlug(), "path", in.GetPath(), "branch", in.GetBranch())
	return file.FindFile(ctx, findFileIn, log)
}

// IsLatestFile lets you know if the object_id is from the latest branch/ref.
func (h *handler) IsLatestFile(ctx context.Context, in *pb.IsLatestFileRequest) (*pb.IsLatestFileResponse, error) {
	log := h.log
	findFileIn := &pb.GetFileRequest{
		Slug:     in.GetSlug(),
		Path:     in.GetPath(),
		Provider: in.GetProvider(),
	}
	if in.GetBranch() != "" {
		findFileIn.Type = &pb.GetFileRequest_Branch{
			Branch: in.GetBranch(),
		}
	} else {
		findFileIn.Type = &pb.GetFileRequest_Ref{
			Ref: in.GetRef(),
		}
	}

	log.Infow("IsLatestFile using FindFile", "slug", in.Slug, "path", in.Path)
	response, err := file.FindFile(ctx, findFileIn, log)
	if err != nil {
		log.Errorw("IsLatestFile failure", "slug ", in.GetSlug(), "path", in.GetPath(), "ref", in.GetRef(), "branch", in.GetBranch(), zap.Error(err))
		return nil, err
	}
	var match bool
	// github uses blob id for update check, others use commit id
	switch findFileIn.GetProvider().Hook.(type) {
	case *pb.Provider_Github:
		match = in.GetBlobId() == response.GetBlobId()
	default:
		match = in.GetBlobId() == response.GetCommitId()
	}
	out := &pb.IsLatestFileResponse{
		Latest: match,
	}
	return out, nil
}

// GetBatchFile is used to return multiple files
func (h *handler) GetBatchFile(ctx context.Context, in *pb.GetBatchFileRequest) (*pb.FileBatchContentResponse, error) {
	return file.BatchFindFile(ctx, in, h.log)
}

// UpdateFile is used to update a file
func (h *handler) UpdateFile(ctx context.Context, in *pb.FileModifyRequest) (*pb.UpdateFileResponse, error) {
	return file.UpdateFile(ctx, in, h.log)
}

// PushFile is used to create a file if it doesnt exist, or update the file if it does.
func (h *handler) PushFile(ctx context.Context, in *pb.FileModifyRequest) (*pb.FileContent, error) {
	return file.PushFile(ctx, in, h.log)
}

// FindFilesInBranch is used to return a list of files in a given branch.
func (h *handler) FindFilesInBranch(ctx context.Context, in *pb.FindFilesInBranchRequest) (*pb.FindFilesInBranchResponse, error) {
	return file.FindFilesInBranch(ctx, in, h.log)
}

// FindFilesInCommit is used to return a list of files in a given commit.
func (h *handler) FindFilesInCommit(ctx context.Context, in *pb.FindFilesInCommitRequest) (*pb.FindFilesInCommitResponse, error) {
	return file.FindFilesInCommit(ctx, in, h.log)
}

// GetLatestCommit returns the latest commit_id for a branch.
func (h *handler) GetLatestCommit(ctx context.Context, in *pb.GetLatestCommitRequest) (*pb.GetLatestCommitResponse, error) {
	return git.GetLatestCommit(ctx, in, h.log)
}

// ListBranches is used to return a list of commit ids given a ref or branch.
func (h *handler) ListBranches(ctx context.Context, in *pb.ListBranchesRequest) (*pb.ListBranchesResponse, error) {
	return git.ListBranches(ctx, in, h.log)
}

// ListCommits is used to return a list of commit ids given a ref or branch.
func (h *handler) ListCommits(ctx context.Context, in *pb.ListCommitsRequest) (*pb.ListCommitsResponse, error) {
	return git.ListCommits(ctx, in, h.log)
}
