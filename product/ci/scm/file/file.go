package file

import (
	"context"
	"crypto/tls"
	"fmt"
	"net/http"
	"time"

	"github.com/drone/go-scm/scm"
	"github.com/drone/go-scm/scm/driver/github"
	"github.com/drone/go-scm/scm/transport/oauth2"
	"github.com/wings-software/portal/commons/go/lib/utils"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

func oauthTransport(token string, skip bool) http.RoundTripper {
	return &oauth2.Transport{
		Base: defaultTransport(skip),
		Source: oauth2.StaticTokenSource(
			&scm.Token{
				Token: token,
			},
		),
	}
}

// defaultTransport provides a default http.Transport. If
// skip verify is true, the transport will skip ssl verification.
func defaultTransport(skip bool) http.RoundTripper {
	return &http.Transport{
		Proxy: http.ProxyFromEnvironment,
		TLSClientConfig: &tls.Config{
			InsecureSkipVerify: skip,
		},
	}
}

func getGitClient(p pb.Provider) (client *scm.Client, err error) {
	switch p.Hook.(type) {
	case *pb.Provider_Github:
		if p.Endpoint == "" {
			client = github.NewDefault()
		} else {
			client, _ = github.New(p.Endpoint)
		}
		client.Client = &http.Client{
			Transport: oauthTransport(p.GetGithub().GetAccessToken(), true),
		}

		return client, nil
	default:
		return nil, status.Errorf(codes.InvalidArgument,
			fmt.Sprintf("Unsupported git provider %s", p.String()))
	}
}

func getValidRef(inputRef string, inputBranch string) (string, error) {
	if inputRef != "" {
		return inputRef, nil
	} else if inputBranch != "" {
		return scm.ExpandRef(inputBranch, "refs/heads"), nil
	} else {
		return "", status.Error(codes.InvalidArgument, "Must provide a ref or a branch")
	}
}

// FindFile returns the contents of a file based on a ref or branch.
func FindFile(ctx context.Context, fileRequest *pb.FileFindRequest, log *zap.SugaredLogger) (out *pb.ContentResponse, err error) {
	start := time.Now()
	log.Infow("FindFile starting", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath())

	client, err := getGitClient(*fileRequest.GetProvider())
	if err != nil {
		log.Errorw("FindFile failure, bad provider", *fileRequest.GetProvider(), "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start), "error", err)
		return nil, err
	}

	ref, refError := getValidRef(fileRequest.GetRef(), fileRequest.GetBranch())
	if refError != nil {
		log.Errorw("Findfile failure, bad ref/branch", "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), "error", err)
		return nil, refError
	}

	response, _, err := client.Contents.Find(ctx, fileRequest.GetSlug(), fileRequest.GetPath(), ref)
	if err != nil {
		log.Errorw("Findfile failure", "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "Hash", response.Hash, "elapsed_time_ms", utils.TimeSince(start))
		return nil, err
	}
	log.Infow("Findfile success", "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "Hash", response.Hash, "elapsed_time_ms", utils.TimeSince(start))
	out = &pb.ContentResponse{
		Data: string(response.Data),
	}
	return out, nil
}

// DeleteFile removes a file, based on a ref or branch. NB not many git vendors have this functionality.
func DeleteFile(ctx context.Context, fileRequest *pb.FileDeleteRequest, log *zap.SugaredLogger) (out *pb.ContentResponse, err error) {
	start := time.Now()
	log.Infow("DeleteFile starting", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath())

	client, err := getGitClient(*fileRequest.GetProvider())
	if err != nil {
		log.Errorw("DeleteFile failure, bad provider", *fileRequest.GetProvider(), "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start), "error", err)
		return nil, err
	}

	ref, refError := getValidRef(fileRequest.GetRef(), fileRequest.GetBranch())
	if refError != nil {
		log.Errorw("Deletefile failure, bad ref/branch", "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), "error", err)
		return nil, refError
	}
	inputParams := new(scm.ContentParams)
	inputParams.Branch = fileRequest.GetBranch()
	inputParams.Ref = ref

	response, err := client.Contents.Delete(ctx, fileRequest.GetSlug(), fileRequest.GetPath(), fileRequest.GetRef())
	if err != nil {
		log.Errorw("DeleteFile failure", "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start))
		return nil, err
	}
	log.Infow("DeleteFile success", "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start))
	out = &pb.ContentResponse{
		Status: int32(response.Status),
	}
	return out, nil
}

// UpdateFile updates a file contents, A valid SHA is needed.
func UpdateFile(ctx context.Context, fileRequest *pb.FileModifyRequest, log *zap.SugaredLogger) (out *pb.ContentResponse, err error) {
	start := time.Now()
	log.Infow("UpdateFile starting", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath())

	client, err := getGitClient(*fileRequest.GetProvider())
	if err != nil {
		log.Errorw("UpdateFile failure, bad provider", *fileRequest.GetProvider(), "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start), "error", err)
		return nil, err
	}

	ref, refError := getValidRef(fileRequest.GetRef(), fileRequest.GetBranch())
	if refError != nil {
		log.Errorw("Deletefile failure, bad ref/branch", "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), "error", err)
		return nil, refError
	}
	inputParams := new(scm.ContentParams)
	inputParams.Data = []byte(fileRequest.GetData())
	inputParams.Message = fileRequest.GetMessage()
	inputParams.Branch = fileRequest.GetBranch()
	inputParams.Sha = fileRequest.GetSha()
	inputParams.Ref = ref
	inputParams.Signature = scm.Signature{
		Name:  fileRequest.GetSignature().Name,
		Email: fileRequest.GetSignature().Email,
	}
	response, err := client.Contents.Update(ctx, fileRequest.GetSlug(), fileRequest.GetPath(), inputParams)
	if err != nil {
		log.Errorw("UpdateFile failure", "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "sha", inputParams.Sha, "branch", inputParams.Branch, "elapsed_time_ms", utils.TimeSince(start))
		return nil, err
	}
	log.Infow("UpdateFile success", "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "sha", inputParams.Sha, "branch", inputParams.Branch, "elapsed_time_ms", utils.TimeSince(start))
	out = &pb.ContentResponse{
		Status: int32(response.Status),
	}
	return out, nil

}

// UpsertFile creates a file if it does not exist, otherwise it updates it.
func UpsertFile(ctx context.Context, fileRequest *pb.FileModifyRequest, log *zap.SugaredLogger) (out *pb.ContentResponse, err error) {
	start := time.Now()
	log.Infow("UpsertFile starting", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath())

	client, err := getGitClient(*fileRequest.GetProvider())
	if err != nil {
		log.Errorw("UpsertFile failure, bad provider", *fileRequest.GetProvider(), "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start), "error", err)
		return nil, err
	}

	ref, refError := getValidRef(fileRequest.GetRef(), fileRequest.GetBranch())
	if refError != nil {
		log.Errorw("Deletefile failure, bad ref/branch", "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "ref", ref, "elapsed_time_ms", utils.TimeSince(start), "error", err)
		return nil, refError
	}

	file, _, err := client.Contents.Find(ctx, fileRequest.GetSlug(), fileRequest.GetPath(), ref)
	if err == nil {
		log.Infow("UpsertFile calling UpdateFile", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath())
		fileRequest.Sha = file.Hash
		return UpdateFile(ctx, fileRequest, log)
	} else {
		log.Infow("UpsertFile calling CreateFile", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath())
		return CreateFile(ctx, fileRequest, log)
	}
}

// CreateFile creates a file with the passed through contents, it will fail if the file already exists.
func CreateFile(ctx context.Context, fileRequest *pb.FileModifyRequest, log *zap.SugaredLogger) (out *pb.ContentResponse, err error) {
	start := time.Now()
	log.Infow("CreateFile starting", "slug", fileRequest.GetSlug(), "path", fileRequest.GetPath())

	client, err := getGitClient(*fileRequest.GetProvider())
	if err != nil {
		log.Errorw("CreateFile failure, bad provider", *fileRequest.GetProvider(), "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "elapsed_time_ms", utils.TimeSince(start), "error", err)
		return nil, err
	}

	inputParams := new(scm.ContentParams)
	inputParams.Data = []byte(fileRequest.GetData())
	inputParams.Message = fileRequest.GetMessage()
	inputParams.Branch = fileRequest.GetBranch()
	inputParams.Signature = scm.Signature{
		Name:  fileRequest.GetSignature().Name,
		Email: fileRequest.GetSignature().Email,
	}
	response, err := client.Contents.Create(ctx, fileRequest.GetSlug(), fileRequest.GetPath(), inputParams)
	if err != nil {
		log.Errorw("CreateFile failure", "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "branch", inputParams.Branch, "elapsed_time_ms", utils.TimeSince(start))
		return nil, err
	}
	log.Infow("CreateFile success", "using slug ", fileRequest.GetSlug(), "path", fileRequest.GetPath(), "branch", inputParams.Branch, "elapsed_time_ms", utils.TimeSince(start))
	out = &pb.ContentResponse{
		Status: int32(response.Status),
	}
	return out, nil
}
