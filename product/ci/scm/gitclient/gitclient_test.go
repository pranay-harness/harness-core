package gitclient

import (
	"testing"
)

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
			got, err := GetValidRef(tt.args.inputRef, tt.args.inputBranch)
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
