package parser

import (
	"context"
	"io/ioutil"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
)

func TestParseWebhookPRSuccess(t *testing.T) {
	raw, _ := ioutil.ReadFile("testdata/pr.json")
	in := &pb.ParseWebhookRequest{
		Body: string(raw),
		Header: &pb.Header{
			Fields: []*pb.Header_Pair{
				{
					Key:    "X-Github-Event",
					Values: []string{"pull_request"},
				},
			},
		},
		Secret:   "",
		Provider: pb.GitProvider_GITHUB,
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ret, err := ParseWebhook(context.Background(), in, log.Sugar())
	assert.Nil(t, err)
	assert.NotNil(t, ret.GetPr())
	assert.Equal(t, ret.GetPr().GetAction(), pb.Action_OPEN)
}

func TestParseWebhook_UnknownActionErr(t *testing.T) {
	raw, _ := ioutil.ReadFile("testdata/pr.json")
	in := &pb.ParseWebhookRequest{
		Body: string(raw),
		Header: &pb.Header{
			Fields: []*pb.Header_Pair{
				{
					Key:    "X-Github-Event",
					Values: []string{"test"},
				},
			},
		},
		Secret:   "",
		Provider: pb.GitProvider_GITHUB,
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	_, err := ParseWebhook(context.Background(), in, log.Sugar())
	assert.NotNil(t, err)
}

func TestParseWebhook_UnknownErr(t *testing.T) {
	raw, _ := ioutil.ReadFile("testdata/pr.err.json")
	in := &pb.ParseWebhookRequest{
		Body: string(raw),
		Header: &pb.Header{
			Fields: []*pb.Header_Pair{
				{
					Key:    "X-Github-Event",
					Values: []string{"pull_request"},
				},
			},
		},
		Secret:   "",
		Provider: pb.GitProvider_GITHUB,
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ret, err := ParseWebhook(context.Background(), in, log.Sugar())
	assert.Nil(t, err)
	assert.Equal(t, ret.GetPr().GetAction(), pb.Action_UNKNOWN)
}
