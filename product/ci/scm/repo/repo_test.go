// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package repo

import (
	"context"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
)

func TestCreateWebhook(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(201)
		content, _ := ioutil.ReadFile("testdata/webhook.json")
		fmt.Fprint(w, string(content))
	}))
	defer ts.Close()

	in := &pb.CreateWebhookRequest{
		Slug:       "octocat/hello-world",
		Name:       "drone",
		Target:     "https://example.com",
		Secret:     "topsecret",
		SkipVerify: true,
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
					},
				},
			},
			Endpoint: ts.URL,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := CreateWebhook(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, int32(201), got.Status, "Correct http response")
	assert.Equal(t, "1", got.Webhook.Id, "ID matches")
}

func TestDeleteWebhook(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(204)
	}))
	defer ts.Close()

	in := &pb.DeleteWebhookRequest{
		Slug: "octocat/hello-world",
		Id:   "drone",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
					},
				},
			},
			Endpoint: ts.URL,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := DeleteWebhook(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, int32(204), got.Status, "Correct http response")
}

func TestListWebhooks(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		content, _ := ioutil.ReadFile("testdata/webhooks.json")
		fmt.Fprint(w, string(content))
	}))
	defer ts.Close()

	in := &pb.ListWebhooksRequest{
		Slug: "octocat/hello-world",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
					},
				},
			},
			Endpoint: ts.URL,
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := ListWebhooks(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, 1, len(got.Webhooks), "one webhook")
	assert.Equal(t, int32(0), got.Pagination.Next, "No next page")
}
