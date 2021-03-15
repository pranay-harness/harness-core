package file

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

func TestFindFileNegativePath(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		content, _ := ioutil.ReadFile("testdata/FileErrorSource.json")
		fmt.Fprint(w, content)
	}))
	defer ts.Close()

	in := &pb.FileFindRequest{
		Slug: "tphoney/scm-test",
		Path: "jello",
		Type: &pb.FileFindRequest_Branch{
			Branch: "main",
		},
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
	got, err := FindFile(context.Background(), in, log.Sugar())

	assert.NotNil(t, err, "error thrown")
	assert.Nil(t, got, "Nothing returned")
}
func TestCreateFile(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(201)
		content, _ := ioutil.ReadFile("testdata/FileCreateSource.json")
		fmt.Fprint(w, content)
	}))
	defer ts.Close()

	in := &pb.FileModifyRequest{
		Slug:    "tphoney/scm-test",
		Path:    "jello",
		Message: "message",
		Type: &pb.FileModifyRequest_Branch{
			Branch: "main",
		},
		Data: "data",
		Signature: &pb.Signature{
			Name:  "tp honey",
			Email: "tp@harness.io",
		},
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
	got, err := CreateFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, got.Status, int32(201), "status matches")
}

func TestUpdateFile(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		content, _ := ioutil.ReadFile("testdata/FileUpdateSource.json")
		fmt.Fprint(w, content)
	}))
	defer ts.Close()

	in := &pb.FileModifyRequest{
		Slug:    "tphoney/scm-test",
		Path:    "jello",
		Message: "message",
		Type: &pb.FileModifyRequest_Branch{
			Branch: "main",
		},
		Data: "data",
		Sha:  "4ea5e4dd2666245c95ea7d4cd353182ea19934b3",
		Signature: &pb.Signature{
			Name:  "tp honey",
			Email: "tp@harness.io",
		},
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
	got, err := UpdateFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, got.Status, int32(200), "status matches")
}

func TestDeleteFile(t *testing.T) {
	in := &pb.FileDeleteRequest{
		Slug: "tphoney/scm-test",
		Path: "jello",
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
					},
				},
			},
			Endpoint: "https://localhost:8081",
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	_, err := DeleteFile(context.Background(), in, log.Sugar())

	assert.NotNil(t, err, "throws an error")
}
func TestUpsertNewFile(t *testing.T) {
	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(200)
		if r.Method == http.MethodGet {
			content, _ := ioutil.ReadFile("testdata/FileError.json")
			fmt.Fprint(w, content)
		} else {
			content, _ := ioutil.ReadFile("testdata/FileCreateSource.json")
			fmt.Fprint(w, content)
		}
	}))
	defer ts.Close()

	in := &pb.FileModifyRequest{
		Slug:    "tphoney/scm-test",
		Path:    "jello",
		Message: "message",
		Type: &pb.FileModifyRequest_Branch{
			Branch: "main",
		},
		Data: "data",
		Signature: &pb.Signature{
			Name:  "tp honey",
			Email: "tp@harness.io",
		},
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
	got, err := UpsertFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, got.Status, int32(200), "status matches")
}

func TestFindFileRealRequest(t *testing.T) {
	in := &pb.FileFindRequest{
		Slug: "tphoney/scm-test",
		Path: "README.md",
		Type: &pb.FileFindRequest_Branch{
			Branch: "main",
		},
		Provider: &pb.Provider{
			Hook: &pb.Provider_Github{
				Github: &pb.GithubProvider{
					Provider: &pb.GithubProvider_AccessToken{
						AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
					},
				},
			},
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	got, err := FindFile(context.Background(), in, log.Sugar())

	assert.Nil(t, err, "no errors")
	assert.Equal(t, got.Status, int32(0), "status matches")
	assert.Contains(t, got.Data, "test repo for source control operations")
}

func Test_getValidRef(t *testing.T) {
	type args struct {
		inputRef    string
		inputBranch string
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "use ref",
			args: args{
				inputRef:    "bla",
				inputBranch: "",
			},
			want:    "bla",
			wantErr: false,
		},
		{
			name: "use branch",
			args: args{
				inputRef:    "",
				inputBranch: "foo",
			},
			want:    "refs/heads/foo",
			wantErr: false,
		},
		{
			name: "error if no valid args",
			args: args{
				inputRef:    "",
				inputBranch: "",
			},
			want:    "",
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := getValidRef(tt.args.inputRef, tt.args.inputBranch)
			if (err != nil) != tt.wantErr {
				t.Errorf("getValidRef() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("getValidRef() = %v, want %v", got, tt.want)
			}
		})
	}
}
