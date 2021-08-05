package visualization

import (
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	cgp "github.com/wings-software/portal/product/ci/addon/parser/cg"
	"go.uber.org/zap"
)

func TestNewVisGraphParser(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewOSFileSystem(log.Sugar())
	vis := NewVisGraphParser(log.Sugar(), fs)
	dto, _ := vis.Parse([]string{"testdata/vis.csv"})

	k := cgp.NewNode(-1005937475, "Source", "io.logz.sender.com.google.gson.stream", "JsonWriter", "beginObject", "()")
	v := cgp.NewNode(1471685929, "Source", "io.logz.sender.com.google.gson.stream", "JsonWriter", "open", "(int,java.lang.String)")

	assert.Equal(t, len(dto.Values), 10)
	assert.Equal(t, len(dto.Keys), 10)
	assert.True(t, reflect.DeepEqual(*k, dto.Keys[0]))
	assert.True(t, reflect.DeepEqual(*v, dto.Values[0]))
}
