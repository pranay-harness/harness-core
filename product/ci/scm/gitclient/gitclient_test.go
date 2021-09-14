// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package gitclient

import (
	"testing"

	pb "github.com/wings-software/portal/product/ci/scm/proto"
)

func Test_getValidRef(t *testing.T) {
	type args struct {
		provider    pb.Provider
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
				provider: pb.Provider{
					Hook: &pb.Provider_Github{
						Github: &pb.GithubProvider{
							Provider: &pb.GithubProvider_AccessToken{
								AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
							},
						},
					},
				},
				inputRef:    "bla",
				inputBranch: "",
			},
			want:    "bla",
			wantErr: false,
		},
		{
			name: "use branch",
			args: args{
				provider: pb.Provider{
					Hook: &pb.Provider_Github{
						Github: &pb.GithubProvider{
							Provider: &pb.GithubProvider_AccessToken{
								AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
							},
						},
					},
				},
				inputRef:    "",
				inputBranch: "foo",
			},
			want:    "refs/heads/foo",
			wantErr: false,
		},
		{
			name: "use bitbucket",
			args: args{
				provider: pb.Provider{
					Hook: &pb.Provider_BitbucketCloud{
						BitbucketCloud: &pb.BitbucketCloudProvider{
							Username:    "",
							AppPassword: "",
						},
					},
				},
				inputRef:    "",
				inputBranch: "foo",
			},
			want:    "foo",
			wantErr: false,
		},
		{
			name: "error if no valid args",
			args: args{
				provider: pb.Provider{
					Hook: &pb.Provider_Github{
						Github: &pb.GithubProvider{
							Provider: &pb.GithubProvider_AccessToken{
								AccessToken: "963408579168567c07ff8bfd2a5455e5307f74d4",
							},
						},
					},
				},
				inputRef:    "",
				inputBranch: "",
			},
			want:    "",
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := GetValidRef(tt.args.provider, tt.args.inputRef, tt.args.inputBranch)
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
