package parser

import (
	"context"
	"io/ioutil"
	"testing"

	"github.com/golang/protobuf/jsonpb"
	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
)

func TestParsePRWebhookPRSuccess(t *testing.T) {
	data, _ := ioutil.ReadFile("testdata/pr.json")
	in := &pb.ParseWebhookRequest{
		Body: string(data),
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
	got, err := ParseWebhook(context.Background(), in, log.Sugar())
	assert.Nil(t, err)

	want := &pb.ParseWebhookResponse{}
	raw, _ := ioutil.ReadFile("testdata/pr.json.golden")
	jsonpb.UnmarshalString(string(raw), want)

	if !proto.Equal(got, want) {
		t.Errorf("Unexpected Results")
		t.Log(got)
		t.Log(want)
	}
}

func TestParsePRWebhook_UnknownActionErr(t *testing.T) {
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

func TestParsePRWebhook_UnknownErr(t *testing.T) {
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

func TestParsePushWebhookPRSuccess(t *testing.T) {
	data, _ := ioutil.ReadFile("testdata/push.json")
	in := &pb.ParseWebhookRequest{
		Body: string(data),
		Header: &pb.Header{
			Fields: []*pb.Header_Pair{
				{
					Key:    "X-Github-Event",
					Values: []string{"push"},
				},
			},
		},
		Secret:   "",
		Provider: pb.GitProvider_GITHUB,
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := ParseWebhook(context.Background(), in, log.Sugar())
	assert.Nil(t, err)
	assert.NotNil(t, got.GetPush())

	want := &pb.ParseWebhookResponse{}
	raw, _ := ioutil.ReadFile("testdata/push.json.golden")
	jsonpb.UnmarshalString(string(raw), want)
	if !proto.Equal(got, want) {
		t.Errorf("Unexpected Results")
		t.Log(got)
		t.Log(want)
	}
}
