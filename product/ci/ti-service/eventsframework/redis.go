package eventsframework

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"encoding/base64"
	"github.com/go-redis/redis/v7"
	"github.com/robinjoseph08/redisqueue/v2"
	"io/ioutil"
	"strings"
	"time"

	"github.com/golang/protobuf/proto"
	pb "github.com/wings-software/portal/953-events-api/src/main/proto/io/harness/eventsframework/schemas/webhookpayloads"
	scmpb "github.com/wings-software/portal/product/ci/scm/proto"
	"github.com/wings-software/portal/product/ci/ti-service/db"
	"github.com/wings-software/portal/product/ci/ti-service/logger"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.uber.org/zap"
)

type MergeCallbackFn func(ctx context.Context, req types.MergePartialCgRequest) error

type RedisBroker struct {
	consumers     map[string]*ConsumerInfo
	log           *zap.SugaredLogger
	errorInterval time.Duration
	consumerOpts  *redisqueue.ConsumerOptions
}

type ConsumerInfo struct {
	Topic    string
	Fn       func(msg *redisqueue.Message) error
	Consumer *redisqueue.Consumer
}

const (
	defaultErrorInterval = 1 * time.Second
)

type RedisBrokerConf struct {
	Address            string
	UseSentinel        bool
	SentinelMasterName string
	SentinelUrls       string
	UseCluster         bool
	ClusterUrls        string
	Password           string
	UseTLS             bool
	TLSCaFilePath      string
}

func NewRedisBroker(conf RedisBrokerConf, log *zap.SugaredLogger) (*RedisBroker, error) {
	log.Debug("NewRedisBroker")
	redisClient := NewRedisClientFromConfig(conf.Address, conf.UseSentinel, conf.SentinelMasterName, conf.SentinelUrls,
		conf.UseCluster, conf.ClusterUrls, conf.Password, conf.UseTLS, conf.TLSCaFilePath, log)
	log.Debugf("Redis Broker config: %+v", conf)
	opt := redisqueue.ConsumerOptions{
		VisibilityTimeout: 60 * time.Second,
		BlockingTimeout:   5 * time.Second,
		ReclaimInterval:   5 * time.Second,
		BufferSize:        100,
		Concurrency:       5,
		RedisClient:       redisClient,
	}

	return &RedisBroker{consumers: map[string]*ConsumerInfo{},
		log:           log,
		errorInterval: defaultErrorInterval,
		consumerOpts:  &opt}, nil
}

// Using code from FF
// https://github.com/wings-software/ff-server/blob/master/pkg/concurent/redis.go
func NewRedisClientFromConfig(Address string, useSentinel bool, sentinelMaster string, sentinelURLs string,
	useCluster bool, clusterURLs string, password string, useTLS bool, certPathForTLS string, log *zap.SugaredLogger) redis.UniversalClient {
	log.Infof("NewRedisClientFromConfig")
	var redisClient redis.UniversalClient

	if useSentinel {
		log.Infof("Sentinel enabled for redis, so setting it up for URLs: %s", sentinelURLs)
		log.Infof("Sentinel master: %s", sentinelMaster)
		opt := redis.FailoverOptions{
			MasterName: sentinelMaster,
		}
		opt.Password = password
		if useTLS {
			log.Info("TLS enabled for redis sentinel, so setting it up")
			log.Debugf("certPathForTLS: %s", certPathForTLS)
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
	} else if useCluster {
		log.Infof("Cluster enabled for redis, so setting it up for URLs: %s", clusterURLs)
		clusterURLsArray := strings.Split(clusterURLs, ",")
		opt := redis.UniversalOptions{
			Addrs: clusterURLsArray,
		}
		opt.Password = password

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

		redisClient = newRedisClusterClient(&opt)
	} else {
		log.Infof("Regular redis configured, so setting it up for address: %s", Address)

		opt := redisqueue.RedisOptions{Addr: Address}
		opt.Password = password

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

	roots := x509.NewCertPool()
	ok := roots.AppendCertsFromPEM(rootPem)
	if !ok {
		log.Errorf("error adding cert (%s) to pool, error: %s", certPathForTLS, err.Error())
		return nil, err
	}
	return &tls.Config{RootCAs: roots}, nil
}

// Using code from FF
// https://github.com/wings-software/ff-server/blob/master/pkg/concurent/redis.go
func (r *RedisBroker) handleConsumerError(topic string, err error) {
	r.log.Errorw("[redis stream]: (%s) err: %+v\n", "topic", topic, zap.Error(err))
	if strings.Contains(err.Error(), "NOGROUP") || strings.Contains(err.Error(), "MOVED") {
		// there was likely a failover and the groups got das boot.
		r.log.Errorw("[redis stream]: possible failover occuring")
		go r.restartConsumer(topic)
		time.Sleep(time.Second)
	} else if strings.Contains(err.Error(), "CLUSTERDOWN") || strings.Contains(err.Error(), "connection refused") {
		r.log.Errorw("[redis stream]: possible cluster issue")
		// sleep to allow cluster to get it's sorted
		time.Sleep(10 * time.Second)
		go r.restartAllConsumers()
	} else {
		time.Sleep(r.errorInterval)
	}
}

func (r *RedisBroker) restartConsumer(topic string) {
	// Restart the consumer
	r.log.Warnw("restarting consumer", "topic", topic)
	consumer, consumerExists := r.consumers[topic]
	if consumerExists {
		consumer.Consumer.Shutdown()
		r.Register(topic, consumer.Fn)
		r.log.Infow("restarted the consumer", "topic", topic)
	}
}

func (r *RedisBroker) restartAllConsumers() {
	r.log.Warnw("restarting all consumers")
	consumers := r.consumers
	for topic := range consumers {
		r.restartConsumer(topic)
	}
}

func (r *RedisBroker) Register(topic string, fn func(msg *redisqueue.Message) error) error {
	//r.consumers[topic].Consumer.Register(topic, fn)
	consumer, err := redisqueue.NewConsumerWithOptions(r.consumerOpts)
	if err != nil {
		return err
	}
	consumer.Register(topic, fn)
	go func() {
		for err := range consumer.Errors {
			r.handleConsumerError(topic, err)
		}
	}()
	_, consumerExists := r.consumers[topic]
	if consumerExists {
		r.consumers[topic].Consumer.Shutdown()
		r.consumers[topic].Fn = fn
		r.consumers[topic].Consumer = consumer
	} else {
		r.consumers[topic] = &ConsumerInfo{Topic: topic,
			Fn:       fn,
			Consumer: consumer,
		}
	}
	go consumer.Run()
	return nil
}

// Callback which will be invoked for merging of partial call graph
func (r *RedisBroker) RegisterMerge(ctx context.Context, topic string, fn MergeCallbackFn, db db.Db) error {
	err := r.Register(topic, r.getCallback(ctx, fn, db))
	if err != nil {
		return err
	}
	return nil
}

func (r *RedisBroker) getCallback(ctx context.Context, fn MergeCallbackFn, db db.Db) func(msg *redisqueue.Message) error {
	return func(msg *redisqueue.Message) error {
		var accountId string
		var webhookResp interface{}
		var ok bool
		if accountId, ok = msg.Values["accountId"].(string); !ok {
			r.log.Errorw("[redis stream]: no account ID found in the stream")
			return nil
		}
		if webhookResp, ok = msg.Values["o"]; !ok {
			r.log.Errorw("[redis stream]: no webhook data found in the stream")
			return nil
		}
		// Try to unmarshal webhookResp using type webhookDTO
		dto := pb.WebhookDTO{}
		decoded, err := base64.StdEncoding.DecodeString(webhookResp.(string))
		if err != nil {
			r.log.Errorw("[redis stream]: could not b64 decode webhook resp data", "account_id", accountId, zap.Error(err))
			return nil
		}
		err = proto.Unmarshal(decoded, &dto)
		if err != nil {
			r.log.Errorw("[redis stream]: could not unmarshal webhook response data", "account_id", accountId, zap.Error(err))
			return nil
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

			// update ctx with log
			log := r.log.With("request-id", sha, "accountId", accountId)

			if repo == "" || source == "" || target == "" || sha == "" {
				// These fields should always be populated
				log.Errorw("[redis stream]: missing information for merge event", "account_id", accountId,
					"repo", repo, "source", source, "target", target, "sha", sha)
				return nil
			}
			log.Infow("[redis stream]: got a merge notification", "account_id", accountId,
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
			log.Infow("[redis stream]: calling merge CG", "account_id", accountId, "repo", repo,
				"source", source, "target", target, "sha", sha, "changed_files", req.Diff.Files)

			err := fn(logger.WithContext(ctx, log), req)
			if err != nil {
				log.Errorw("[redis stream]: could not merge partial call graph to master", "account_id", accountId,
					"repo", repo, "source", source, "target", target, "sha", sha, "changed_files", req.Diff.Files,
					zap.Error(err))
				return nil
			}
			//}
		}
		return nil
	}
}
