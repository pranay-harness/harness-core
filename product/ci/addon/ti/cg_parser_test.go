package ti

import (
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
)

func TestCallGraphParser_Parse(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewOSFileSystem(log.Sugar())
	cgph := NewCallGraphParser(log.Sugar(), fs)
	dto, _ := cgph.Parse("testdata/cg.json")

	// Assert relations is as expected
	exp := map[int][]int{
		-776083018:  []int{-1648419296},
		1062598667:  []int{-1648419296},
		-2078257563: []int{1020759395},
		-1136127725: []int{1020759395},
		-849735784:  []int{1020759395},
		-1954679604: []int{-268233532},
		2139952358:  []int{-1648419296},
		330989721:   []int{-1648419296},
	}
	for _, v := range dto.Relations {
		assert.Equal(t, v.Tests, exp[v.Source])
	}

	// Assert the length of the Nodes parsed
	assert.Equal(t, len(dto.Nodes), 11)

	// Validate if a sepcific node exisits in the parsed list
	sNode := Node{
		Package: "io.haness.exception",
		Method:  "<init>",
		ID:      2139952358,
		Params:  "java.lang.Sting,java.util.EnumSet",
		Class:   "InvalidAgumentsException",
		Type:    "source",
	}

	contains := false
	for _, node := range dto.Nodes {
		if node == sNode {
			contains = true
		}
	}
	assert.True(t, contains)
}

func TestCallGraphParser_ParseShouldFail(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewOSFileSystem(log.Sugar())
	cgph := NewCallGraphParser(log.Sugar(), fs)
	_, err := cgph.Parse("testdata/cg_invalid.json")

	assert.NotEqual(t, nil, err)
	assert.True(t, strings.Contains(err.Error(), "data unmarshalling to json failed for line"))
}

func TestCallGraphParser_ParseShouldFailNoFile(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewOSFileSystem(log.Sugar())
	cgph := NewCallGraphParser(log.Sugar(), fs)
	_, err := cgph.Parse("testdata/cg_random.json")

	assert.NotEqual(t, nil, err)
	strings.Contains(err.Error(), "failed to open file")
	assert.True(t, strings.Contains(err.Error(), "data unmarshalling to json failed for line"))
}
