package file

import (
	"context"
	"time"

	"github.com/drone/go-scm/scm"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/scm/gitclient"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
)

// FindFile returns the contents of a file based on a ref or branch.
func FindFile(ctx context.Context, fileRequest *pb.GetFileRequest, log *zap.SugaredLogger) (out *pb.FileContent, err error) {
	start := time.Now()
	log.Infow("FindFile starting", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath())

	client, err := gitclient.GetGitClient(*fileRequest.GetProvider(), log)
	if err != nil {
		log.Errorw("FindFile failure", "provider", *fileRequest.GetProvider(), "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	ref, err := gitclient.GetValidRef(*fileRequest.GetProvider(), fileRequest.GetRef(), fileRequest.GetBranch())
	if err != nil {
		log.Errorw("Findfile failure, bad ref/branch", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	response, _, err := client.Contents.Find(ctx, fileRequest.GetSlug(), fileRequest.GetPath(), ref)
	if err != nil {
		log.Errorw("Findfile failure", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("Findfile success", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "commit id", response.Sha, "blob id", response.BlobID, "elapsed_time_ms", utils.TimeSince(start))
	out = &pb.FileContent{
		Content:  string(response.Data),
		CommitId: string(response.Sha),
		BlobId:   string(response.BlobID),
		Path:     fileRequest.Path,
	}
	return out, nil
}

// BatchFindFile returns the contents of a file based on a ref or branch.
func BatchFindFile(ctx context.Context, fileRequests *pb.GetBatchFileRequest, log *zap.SugaredLogger) (out *pb.FileBatchContentResponse, err error) {
	start := time.Now()
	log.Infow("BatchFindFile starting", "files", len(fileRequests.FindRequest))
	var store []*pb.FileContent
	for _, request := range fileRequests.FindRequest {
		file, err := FindFile(ctx, request, log)
		if err != nil {
			log.Errorw("BatchFindFile failure. Unable to get this file", "provider", *request.GetProvider(), "slug", request.GetSlug(), "path", request.GetPath(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
			return nil, err
		}
		store = append(store, file)
	}
	out = &pb.FileBatchContentResponse{
		FileContents: store,
	}
	log.Infow("BatchFindFile complete", "number of files searched for ", len(fileRequests.FindRequest), "elapsed_time_ms", utils.TimeSince(start))
	return out, nil
}

// DeleteFile removes a file, based on a ref or branch. NB not many git vendors have this functionality.
func DeleteFile(ctx context.Context, fileRequest *pb.DeleteFileRequest, log *zap.SugaredLogger) (out *pb.DeleteFileResponse, err error) {
	start := time.Now()
	log.Infow("DeleteFile starting", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath())

	client, err := gitclient.GetGitClient(*fileRequest.GetProvider(), log)
	if err != nil {
		log.Errorw("DeleteFile failure", "bad provider", *fileRequest.GetProvider(), "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	ref, err := gitclient.GetValidRef(*fileRequest.GetProvider(), fileRequest.GetRef(), fileRequest.GetBranch())
	if err != nil {
		log.Errorw("Deletefile failure bad ref/branch", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	inputParams := new(scm.ContentParams)
	inputParams.Branch = fileRequest.GetBranch()
	inputParams.Ref = ref

	response, err := client.Contents.Delete(ctx, fileRequest.GetSlug(), fileRequest.GetPath(), fileRequest.GetRef())
	if err != nil {
		log.Errorw("DeleteFile failure", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("DeleteFile success", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start))
	out = &pb.DeleteFileResponse{
		Status: int32(response.Status),
	}
	return out, nil
}

// UpdateFile updates a file contents, A valid SHA is needed.
func UpdateFile(ctx context.Context, fileRequest *pb.FileModifyRequest, log *zap.SugaredLogger) (out *pb.UpdateFileResponse, err error) {
	start := time.Now()
	log.Infow("UpdateFile starting", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath())

	client, err := gitclient.GetGitClient(*fileRequest.GetProvider(), log)
	if err != nil {
		log.Errorw("UpdateFile failure", "bad provider", *fileRequest.GetProvider(), "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	ref, err := gitclient.GetValidRef(*fileRequest.GetProvider(), fileRequest.GetRef(), fileRequest.GetBranch())
	if err != nil {
		log.Errorw("UpdateFile failure, bad ref/branch", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	inputParams := new(scm.ContentParams)
	inputParams.Data = []byte(fileRequest.GetContent())
	inputParams.Message = fileRequest.GetMessage()
	inputParams.Branch = fileRequest.GetBranch()
	// github uses blob id for update check, others use commit id
	switch fileRequest.GetProvider().Hook.(type) {
	case *pb.Provider_Github:
		inputParams.BlobID = fileRequest.GetBlobId()
	default:
		inputParams.Sha = fileRequest.GetCommitId()
	}

	inputParams.Signature = scm.Signature{
		Name:  fileRequest.GetSignature().GetName(),
		Email: fileRequest.GetSignature().GetEmail(),
	}
	response, err := client.Contents.Update(ctx, fileRequest.GetSlug(), fileRequest.GetPath(), inputParams)
	if err != nil {
		log.Errorw("UpdateFile failure", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "sha", inputParams.Sha, "branch", inputParams.Branch, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("UpdateFile success", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "sha", inputParams.Sha, "branch", inputParams.Branch, "elapsed_time_ms", utils.TimeSince(start))
	out = &pb.UpdateFileResponse{
		Status: int32(response.Status),
	}
	return out, nil

}

// PushFile creates a file if it does not exist, otherwise it updates it.
func PushFile(ctx context.Context, fileRequest *pb.FileModifyRequest, log *zap.SugaredLogger) (out *pb.FileContent, err error) {
	start := time.Now()
	out = &pb.FileContent{}
	log.Infow("PushFile starting", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath())

	client, err := gitclient.GetGitClient(*fileRequest.GetProvider(), log)
	if err != nil {
		log.Errorw("PushFile failure", "bad provider", *fileRequest.GetProvider(), "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	ref, err := gitclient.GetValidRef(*fileRequest.GetProvider(), fileRequest.GetRef(), fileRequest.GetBranch())
	if err != nil {
		log.Errorw("PushFile failure, bad ref/branch", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	file, _, err := client.Contents.Find(ctx, fileRequest.GetSlug(), fileRequest.GetPath(), ref)
	if err == nil {
		log.Infow("PushFile calling UpdateFile", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath())
		// github uses blob id for update check, others use commit id
		switch fileRequest.GetProvider().Hook.(type) {
		case *pb.Provider_Github:
			fileRequest.BlobId = file.BlobID
		default:
			fileRequest.CommitId = file.Sha
		}
		updateResponse, err := UpdateFile(ctx, fileRequest, log)
		if err != nil {
			log.Errorw("PushFile failure, UpdateFile failed", "provider", *fileRequest.GetProvider(), "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
			return nil, err
		}
		out.Status = updateResponse.Status
	} else {
		log.Infow("PushFile calling CreateFile", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath())
		createResponse, err := CreateFile(ctx, fileRequest, log)
		if err != nil {
			log.Errorw("PushFile failure, CreateFile failed", "provider", *fileRequest.GetProvider(), "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
			return nil, err
		}
		out.Status = createResponse.Status
	}
	file, _, err = client.Contents.Find(ctx, fileRequest.GetSlug(), fileRequest.GetPath(), ref)
	if err != nil {
		log.Errorw("Findfile failure", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("UpsertFile success", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start))
	out.Path = fileRequest.GetPath()
	out.CommitId = string(file.Sha)
	out.BlobId = string(file.BlobID)
	out.Content = string(file.Data)

	return out, nil
}

// CreateFile creates a file with the passed through contents, it will fail if the file already exists.
func CreateFile(ctx context.Context, fileRequest *pb.FileModifyRequest, log *zap.SugaredLogger) (out *pb.CreateFileResponse, err error) {
	start := time.Now()
	log.Infow("CreateFile starting", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath())

	client, err := gitclient.GetGitClient(*fileRequest.GetProvider(), log)
	if err != nil {
		log.Errorw("CreateFile failure", "bad provider", *fileRequest.GetProvider(), "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	inputParams := new(scm.ContentParams)
	inputParams.Data = []byte(fileRequest.GetContent())
	inputParams.Message = fileRequest.GetMessage()
	inputParams.Branch = fileRequest.GetBranch()
	inputParams.Signature = scm.Signature{
		Name:  fileRequest.GetSignature().GetName(),
		Email: fileRequest.GetSignature().GetEmail(),
	}
	response, err := client.Contents.Create(ctx, fileRequest.GetSlug(), fileRequest.GetPath(), inputParams)
	if err != nil {
		log.Errorw("CreateFile failure", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "branch", inputParams.Branch, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("CreateFile success", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "branch", inputParams.Branch, "elapsed_time_ms", utils.TimeSince(start))
	out = &pb.CreateFileResponse{
		Status: int32(response.Status),
	}
	return out, nil
}

func FindFilesInBranch(ctx context.Context, fileRequest *pb.FindFilesInBranchRequest, log *zap.SugaredLogger) (out *pb.FindFilesInBranchResponse, err error) {
	start := time.Now()
	log.Infow("FindFilesInBranch starting", "slug", fileRequest.GetSlug())

	client, err := gitclient.GetGitClient(*fileRequest.GetProvider(), log)
	if err != nil {
		log.Errorw("FindFilesInBranch failure", "bad provider", *fileRequest.GetProvider(), "slug", fileRequest.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}

	ref, err := gitclient.GetValidRef(*fileRequest.GetProvider(), "", fileRequest.GetBranch())
	if err != nil {
		log.Errorw("FindFilesInBranch failure, bad ref/branch", "provider", *fileRequest.GetProvider(), "slug", fileRequest.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	response, _, err := client.Contents.List(ctx, fileRequest.GetSlug(), fileRequest.GetPath(), ref, scm.ListOptions{})
	if err != nil {
		log.Errorw("FindFilesInBranch failure", "provider", *fileRequest.GetProvider(), "slug", fileRequest.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("FindFilesInBranch success", "slug", fileRequest.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start))
	out = &pb.FindFilesInBranchResponse{
		File: convertContentList(response),
	}
	return out, nil
}

func FindFilesInCommit(ctx context.Context, fileRequest *pb.FindFilesInCommitRequest, log *zap.SugaredLogger) (out *pb.FindFilesInCommitResponse, err error) {
	start := time.Now()
	log.Infow("FindFilesInCommit starting", "slug", fileRequest.GetSlug())

	client, err := gitclient.GetGitClient(*fileRequest.GetProvider(), log)
	if err != nil {
		log.Errorw("FindFilesInCommit failure", "bad provider", *fileRequest.GetProvider(), "slug", fileRequest.GetSlug(), "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	ref := fileRequest.GetRef()
	response, _, err := client.Contents.List(ctx, fileRequest.GetSlug(), fileRequest.GetPath(), ref, scm.ListOptions{})
	if err != nil {
		log.Errorw("FindFilesInCommit failure", "provider", *fileRequest.GetProvider(), "slug", fileRequest.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return nil, err
	}
	log.Infow("FindFilesInCommit success", "slug", fileRequest.GetSlug(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start))
	out = &pb.FindFilesInCommitResponse{
		File: convertContentList(response),
	}
	return out, nil
}

func convertContentList(from []*scm.ContentInfo) (to []*pb.FileChange) {
	for _, v := range from {
		to = append(to, convertContent(v))
	}
	return to
}

func convertContent(from *scm.ContentInfo) *pb.FileChange {
	returnValue := &pb.FileChange{
		Path:     from.Path,
		CommitId: from.Sha,
		BlobId:   from.BlobID,
	}

	switch from.Kind.String() {
	case "file":
		returnValue.ContentType = pb.ContentType_FILE
	case "directory":
		returnValue.ContentType = pb.ContentType_DIRECTORY
	case "symlink":
		returnValue.ContentType = pb.ContentType_SYMLINK
	case "gitlink":
		returnValue.ContentType = pb.ContentType_GITLINK
	default:
		returnValue.ContentType = pb.ContentType_UNKNOWN_CONTENT
	}
	return returnValue
}
