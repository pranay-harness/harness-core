package eventsframework

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"encoding/base64"
	"errors"
	"github.com/robinjoseph08/redisqueue/v2"
	"io/ioutil"
	"strings"
	"time"

	"github.com/go-redis/redis/v7"
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

func New(endpoint, password string, useSentinel, enableTLS bool, certPath, sentinelMaster, sentinelURLs string, log *zap.SugaredLogger) (*RedisBroker, error) {
	usePassword := password != ""
	client := NewRedisClientFromConfig(endpoint, false, "se", "ds", usePassword,
		password, enableTLS, certPath, log)
	c1, err := redisqueue.NewConsumerWithOptions(&redisqueue.ConsumerOptions{
		RedisClient: client,
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

func NewRedisClientFromConfig(endpoint string, useSentinel bool, sentinelMaster, sentinelURLs string, usePassword bool, password string, useTLS bool, certPathForTLS string, log *zap.SugaredLogger) redis.UniversalClient {
	log.Info("NewRedisClientFromConfig")
	var redisClient redis.UniversalClient

	if useSentinel {
		log.Infow("Sentinel enabled for redis, setting it up", "urls", sentinelURLs)
		log.Infow("Sentinel master", sentinelMaster)
		opt := redis.FailoverOptions{
			MasterName: sentinelMaster,
		}
		if usePassword {
			log.Infow("Using redis password")
			opt.Password = password
		} else {
			log.Infow("Not using redis password")
		}
		if useTLS {
			log.Infow("TLS enabled for redis sentinel, so setting it up")
			newTlSConfig, err := newTlSConfig(certPathForTLS, log)
			if err != nil {
				log.Fatal(err)
			}
			opt.TLSConfig = newTlSConfig
		} else {
			log.Info("TLS not enabled for redis sentinel, so not setting it up")
		}

		sentinelURLsArray := strings.Split(sentinelURLs, ",")
		opt.SentinelAddrs = sentinelURLsArray
		redisClient = newRedisSentinelClient(&opt)
	} else {
		log.Infof("Regular redis configured, so setting it up for address: %s", endpoint)

		opt := redisqueue.RedisOptions{Addr: endpoint}
		if usePassword {
			log.Info("Using redis password")
			opt.Password = password
		} else {
			log.Info("Not using redis password")
		}

		if useTLS {
			log.Info("TLS enabled for redis, so setting it up")
			newTlSConfig, err := newTlSConfig(certPathForTLS, log)
			if err != nil {
				log.Fatal(err)
			}
			opt.TLSConfig = newTlSConfig
		} else {
			log.Info("TLS not enabled for redis, so not setting it up")
		}

		redisClient = newRedisClient(&opt)
	}
	return redisClient
}


func newRedisClient(redisOptions *redis.Options) redis.UniversalClient {
	return redis.NewClient(redisOptions)
}

func newRedisSentinelClient(redisOptions *redis.FailoverOptions) redis.UniversalClient {
	return redis.NewFailoverClient(redisOptions)
}

func newRedisClusterClient(redisOptions *redis.UniversalOptions) redis.UniversalClient {
	return redis.NewUniversalClient(redisOptions)
}

func newTlSConfig(certPathForTLS string, log *zap.SugaredLogger) (*tls.Config, error) {
	// Create TLS config using cert PEM
	rootPem, err := ioutil.ReadFile(certPathForTLS)
	if err != nil {
		log.Errorf("could not read certificate file (%s), error: %s", certPathForTLS, err.Error())
		return nil, err
	}
	//// Base64 decode the PEM file
	//rootPemDec, err := base64.StdEncoding.DecodeString(string(rootPem))
	//if err != nil {
	//	log.Errorf("could not b64 decode cert: %s. error: %s", certPathForTLS, err.Error())
	//	return nil, err
	//}
	roots := x509.NewCertPool()
	ok := roots.AppendCertsFromPEM(rootPem)
	if !ok {
		log.Errorf("error adding cert (%s) to pool, error: %s", certPathForTLS, err.Error())
		return nil, err
	}
	return &tls.Config{RootCAs: roots}, nil
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
		switch dto.GetParsedResponse().Hook.(type) {
		case *scmpb.ParseWebhookResponse_Pr:
			// Check if the PR event payload corresponds to a merge
			pr := dto.GetParsedResponse().GetPr() // scm.PullRequestHook
			merged := pr.GetPr().GetMerged()
			if merged == false {
				return nil
			}
			// We received a merge notification
			repo := pr.GetRepo().GetLink()   // Link to repo eg: https://github.com/wings-software/jhttp
			source := pr.GetPr().GetSource() // Source branch
			target := pr.GetPr().GetTarget() // Target branch for merge
			sha := pr.GetPr().GetSha()       // Sha of the topmost commit in the commits list
			if repo == "" || source == "" || target == "" || sha == "" {
				// These fields should always be populated
				r.log.Errorw("missing information for merge event", "account_id", accountId,
					"repo", repo, "source", source, "target", target, "sha", sha)
				return errors.New("missing information for merge event")
			}
			r.log.Infow("got a merge notification", "account_id", accountId,
				"repo", repo, "source", source, "target", target, "sha", sha)
			req := types.MergePartialCgRequest{AccountId: accountId,
				TargetBranch: target, Repo: repo, Diff: types.DiffInfo{Sha: sha}}
			// Get list of files corresponding to these sha values
			// This needs to be added once DB schema of coverage table is modified
			// to include source branch, target branch, etc.
			// req.Diff, err = db.GetDiffFiles(ctx, accountId, repo, source, sha)
			//if err != nil {
			//	r.log.Errorw("could not get changed files list", "account_id", accountId, "repo", repo,
			//		"source", source, "target", target, "sha", sha, zap.Error(err))
			//	return err
			//}
			// Add this if statement once we start writing files with updated schema
			//if len(req.Diff.Files) != 0 {
			// Found the merge event with changed files
			r.log.Infow("calling merge CG", "account_id", accountId, "repo", repo,
				"source", source, "target", target, "sha", sha, "changed_files", req.Diff.Files)
			err := fn(ctx, req)
			if err != nil {
				r.log.Errorw("could not merge partial call graph to master", "account_id", accountId,
					"repo", repo, "source", source, "target", target, "sha", sha, "changed_files", req.Diff.Files,
					zap.Error(err))
				return err
			}
			//}
		}
		return nil
	}
}
