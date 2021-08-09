package db

import (
	"reflect"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func Test_collapseSpaces(t *testing.T) {
	type args struct {
		s string
	}
	tests := []struct {
		name string
		args args
		want interface{}
	}{
		{
			name: "check1",
			args: args{"select  * from table"},
			want: "select * from table",
		},
		{
			name: "check2",
			args: args{s: "    select      *     from    table        "},
			want: "select * from table",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := collapseSpaces(tt.args.s); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("collapseSpaces() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_hash(t *testing.T) {
	expected := uint32(2124666017)
	assert.Equal(t, expected, hash("select * from table"))
}

func Test_Ms(t *testing.T) {
	expected := float64(60000)
	assert.Equal(t, expected, ms(time.Minute))

	expected = float64(1000)
	assert.Equal(t, expected, ms(time.Second))
}
