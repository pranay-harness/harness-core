package eventsframework

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"encoding/base64"
	"errors"
	"fmt"
	"github.com/robinjoseph08/redisqueue"
	"io/ioutil"
	"strings"
	"time"

	"github.com/golang/protobuf/proto"
	pb "github.com/wings-software/portal/953-events-api/src/main/proto/io/harness/eventsframework/schemas/webhookpayloads"
	scmpb "github.com/wings-software/portal/product/ci/scm/proto"
	"github.com/wings-software/portal/product/ci/ti-service/db"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.uber.org/zap"
)

type MergeCallbackFn func(ctx context.Context, req types.MergePartialCgRequest) error

type RedisBroker struct {
	consumer *redisqueue.Consumer
	log      *zap.SugaredLogger
}

func New(endpoint, password string, enableTLS bool, certPath string, log *zap.SugaredLogger) (*RedisBroker, error) {
	// TODO: (vistaar) Configure with options using values suitable for prod and pass as env variables.
	opt := redisqueue.RedisOptions{Addr: endpoint}
	if password != "" {
		opt.Password = password
	}
	if enableTLS == true {
		// Create TLS config using cert PEM
		rootPem, err := ioutil.ReadFile(certPath)
		if err != nil {
			log.Errorw("could not read certificate file", "path", certPath, zap.Error(err))
			return nil, err
		}

		roots := x509.NewCertPool()
		ok := roots.AppendCertsFromPEM(rootPem)
		if !ok {
			log.Errorw("could not use cert", "path", certPath, zap.Error(err))
			return nil, err
		}
		opt.TLSConfig = &tls.Config{RootCAs: roots}
	}
	c1, err := redisqueue.NewConsumerWithOptions(&redisqueue.ConsumerOptions{
		RedisOptions:      &opt,
		VisibilityTimeout: 60 * time.Second,
		BlockingTimeout:   5 * time.Second,
		ReclaimInterval:   1 * time.Second,
		BufferSize:        100,
		Concurrency:       10})
	if err != nil {
		return nil, err
	}
	go func() {
		for err := range c1.Errors {
			log.Errorw("error in webhook watcher thread", zap.Error(err))
		}
	}()
	return &RedisBroker{consumer: c1, log: log}, nil
}

func (r *RedisBroker) Register(topic string, fn func(msg *redisqueue.Message) error) {
	r.consumer.Register(topic, fn)
}

func (r *RedisBroker) Run() {
	go func() {
		r.consumer.Run()
	}()
}

// Callback which will be invoked for merging of partial call graph
func (r *RedisBroker) RegisterMerge(ctx context.Context, topic string, fn MergeCallbackFn, db db.Db) {
	r.Register(topic, r.getCallback(ctx, fn, db))
}

func (r *RedisBroker) getCallback(ctx context.Context, fn MergeCallbackFn, db db.Db) func(msg *redisqueue.Message) error {
	return func(msg *redisqueue.Message) error {
		var accountId string
		var webhookResp interface{}
		var ok bool
		if accountId, ok = msg.Values["accountId"].(string); !ok {
			r.log.Errorw("no account ID found in the stream")
			return errors.New("account ID not found in the stream")
		}
		if webhookResp, ok = msg.Values["o"]; !ok {
			r.log.Errorw("no webhook data found in the stream")
			return errors.New("webhook data not found in the stream")
		}
		// Try to unmarshal webhookResp using type webhookDTO
		dto := pb.WebhookDTO{}
		decoded, err := base64.StdEncoding.DecodeString(webhookResp.(string))
		if err != nil {
			r.log.Errorw("could not b64 decode webhook resp data", "account_id", accountId, zap.Error(err))
			return err
		}
		err = proto.Unmarshal(decoded, &dto)
		if err != nil {
			r.log.Errorw("could not unmarshal webhook response data", "account_id", accountId, zap.Error(err))
			return errors.New("could not unmarshal webhook response data")
		}
		switch x := dto.GetParsedResponse().Hook.(type) {
		case *scmpb.ParseWebhookResponse_Push:
			repo := dto.GetParsedResponse().GetPush().GetRepo().GetLink()
			p := dto.GetParsedResponse().GetPush().GetRef()
			pList := strings.Split(p, "/")
			branch := pList[len(pList)-1]
			if len(p) == 0 || len(branch) == 0 {
				return nil
			}
			req := types.MergePartialCgRequest{AccountId: accountId, TargetBranch: branch, Repo: repo}
			commitList := []string{}
			for _, c := range dto.GetParsedResponse().GetPush().GetCommits() {
				commitList = append(commitList, c.GetSha())
			}
			// Get list of files corresponding to these sha values
			req.Diff, err = db.GetDiffFiles(ctx, accountId, commitList)
			if err != nil {
				r.log.Errorw("could not get changed files list", "account_id", accountId, "commitList", commitList, "repo", repo, zap.Error(err))
				return err
			}
			if len(req.Diff.Sha) == 0 {
				r.log.Warnw("commit info is empty, skipping merge", "account_id", accountId, "repo", repo)
			} else {
				r.log.Infow("got a push webhook payload", "account_id", accountId,
					"repo", repo, "commit_file_mapping", req.Diff,
					"branch", req.TargetBranch)
				err := fn(ctx, req)
				if err != nil {
					r.log.Errorw("could not merge partial call graph to master", "account_id", accountId,
						"repo", repo, "branch", req.TargetBranch, zap.Error(err))
					return err
				}
			}
		default:
			r.log.Errorw("webhook request is not of push type", "type", x)
			return fmt.Errorf("webhook request is not of push type: %T", x)
		}
		return nil
	}
}
