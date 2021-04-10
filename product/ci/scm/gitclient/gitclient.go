package gitclient

import (
	"crypto/tls"
	"fmt"
	"net/http"
	"net/http/httputil"

	"github.com/drone/go-scm/scm"
	"github.com/drone/go-scm/scm/driver/bitbucket"
	"github.com/drone/go-scm/scm/driver/gitea"
	"github.com/drone/go-scm/scm/driver/github"
	"github.com/drone/go-scm/scm/driver/gitlab"
	"github.com/drone/go-scm/scm/transport"

	"github.com/drone/go-scm/scm/transport/oauth2"
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

func giteaTransport(token string, skip bool) http.RoundTripper {
	return &oauth2.Transport{
		Base:   defaultTransport(skip),
		Scheme: oauth2.SchemeBearer,
		Source: oauth2.StaticTokenSource(
			&scm.Token{
				Refresh: token,
				Token:   token,
			},
		),
	}
}

func bitbucketCloudTransport(username string, password string, skip bool) http.RoundTripper {
	return &transport.BasicAuth{
		Base:     defaultTransport(skip),
		Username: username,
		Password: password,
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

func GetValidRef(p pb.Provider, inputRef string, inputBranch string) (string, error) {
	if inputRef != "" {
		return inputRef, nil
	} else if inputBranch != "" {
		switch p.GetHook().(type) {
		case *pb.Provider_BitbucketCloud:
			return inputBranch, nil
		default:
			return scm.ExpandRef(inputBranch, "refs/heads"), nil
		}
	} else {
		return "", status.Error(codes.InvalidArgument, "Must provide a ref or a branch")
	}
}

func GetGitClient(p pb.Provider, log *zap.SugaredLogger) (client *scm.Client, err error) {
	switch p.GetHook().(type) {
	case *pb.Provider_Github:
		if p.GetEndpoint() == "" {
			client = github.NewDefault()
		} else {
			client, err = github.New(p.GetEndpoint())
			if err != nil {
				log.Errorw("GetGitClient failure Github", "endpoint", p.GetEndpoint(), zap.Error(err))
				return nil, err
			}
		}
		var token string
		switch p.GetGithub().GetProvider().(type) {
		case *pb.GithubProvider_AccessToken:
			token = p.GetGithub().GetAccessToken()
		default:
			// generate oauth token from app and private key
			return nil, status.Errorf(codes.Unimplemented, "Github Application not implemented yet")
		}
		client.Client = &http.Client{
			Transport: oauthTransport(token, p.GetSkipVerify()),
		}
	case *pb.Provider_Gitlab:
		if p.GetEndpoint() == "" {
			client = gitlab.NewDefault()
		} else {
			client, err = gitlab.New(p.GetEndpoint())
			if err != nil {
				log.Errorw("GetGitClient failure Gitlab", "endpoint", p.GetEndpoint(), zap.Error(err))
				return nil, err
			}
		}
		var token string
		switch p.GetGitlab().GetProvider().(type) {
		case *pb.GitlabProvider_AccessToken:
			token = p.GetGitlab().GetAccessToken()
		default:
			return nil, status.Errorf(codes.Unimplemented, "Gitlab personal token not implemented yet")
		}
		client.Client = &http.Client{
			Transport: oauthTransport(token, p.GetSkipVerify()),
		}
	case *pb.Provider_Gitea:
		if p.Endpoint == "" {
			log.Error("getGitClient failure Gitea, endpoint is empty")
			return nil, status.Errorf(codes.InvalidArgument, fmt.Sprintf("Must provide an endpoint for %s", p.String()))
		} else {
			client, err = gitea.New(p.Endpoint)
			if err != nil {
				log.Errorw("GetGitClient failure Gitea", "endpoint", p.Endpoint, zap.Error(err))
				return nil, err
			}
		}
		client.Client = &http.Client{
			Transport: giteaTransport(p.GetGitea().GetAccessToken(), p.GetSkipVerify()),
		}
	case *pb.Provider_BitbucketCloud:
		client = bitbucket.NewDefault()
		client.Client = &http.Client{
			Transport: bitbucketCloudTransport(p.GetBitbucketCloud().GetUsername(), p.GetBitbucketCloud().GetAppPassword(), p.GetSkipVerify()),
		}
	default:
		log.Errorw("GetGitClient unsupported git provider", "endpoint", p.GetEndpoint())
		return nil, status.Errorf(codes.InvalidArgument, "Unsupported git provider")
	}
	if p.Debug {
		client.DumpResponse = func(resp *http.Response, body bool) ([]byte, error) {
			out, err := httputil.DumpResponse(resp, body)
			if err != nil {
				log.Errorw("GetGitClient debug dump failed", "endpoint", p.GetEndpoint())
			}
			log.Infow("GetGitClient debug", "dump", string(out))
			return nil, nil
		}
	}

	return client, nil
}
