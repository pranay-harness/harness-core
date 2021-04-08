package eventsframework

import (
	"context"
	"encoding/base64"
	"errors"
	"fmt"
	"github.com/robinjoseph08/redisqueue"
	"time"

	"github.com/golang/protobuf/proto"
	pb "github.com/wings-software/portal/950-events-api/src/main/proto/io/harness/eventsframework/schemas/webhookpayloads"
	scmpb "github.com/wings-software/portal/product/ci/scm/proto"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.uber.org/zap"
)

type MergeCallbackFn func(ctx context.Context, commits []string, accountId, repo string, files []types.File) error

type RedisBroker struct {
	consumer *redisqueue.Consumer
	log      *zap.SugaredLogger
}

func New(endpoint, password string, log *zap.SugaredLogger) (*RedisBroker, error) {
	// TODO: (vistaar) Configure with options using values suitable for prod and pass as env variables.
	opt := redisqueue.RedisOptions{Addr: endpoint, Password: password}
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
	r.consumer.Run()
}

// Callback which will be invoked for merging of partial call graph
func (r *RedisBroker) RegisterMerge(ctx context.Context, topic string, fn MergeCallbackFn) {
	r.Register(topic, r.getCallback(ctx, fn))
}

func (r *RedisBroker) getCallback(ctx context.Context, fn MergeCallbackFn) func(msg *redisqueue.Message) error {
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
			r.log.Errorw("could not b64 decode webhook resp data", zap.Error(err))
			return err
		}
		err = proto.Unmarshal(decoded, &dto)
		if err != nil {
			r.log.Errorw("could not unmarshal webhook response data")
			return errors.New("could not unmarshal webhook response data")
		}
		switch x := dto.GetParsedResponse().Hook.(type) {
		case *scmpb.ParseWebhookResponse_Push:
			repo := dto.GetParsedResponse().GetPush().GetRepo().GetName()
			commitList := []string{}
			for _, c := range dto.GetParsedResponse().GetPush().GetCommits() {
				commitList = append(commitList, c.GetSha())
			}
			if len(commitList) == 0 {
				r.log.Warnw("no commits received in webhook payload, skipping merge")
			} else {
				// TODO: (vistaar) figure out how to get list of files here
				r.log.Infow("got a push webhook payload", "commitList", commitList,
					"account_id", accountId, "repo", repo)
				err := fn(ctx, commitList, accountId, repo, []types.File{})
				if err != nil {
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
