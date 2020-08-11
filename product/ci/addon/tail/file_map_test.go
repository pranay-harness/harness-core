package tail

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestMultipleCallsToMapping(t *testing.T) {
	x := FileTailMapping()
	y := FileTailMapping()
	// Check both objects are the same
	assert.Equal(t, x, y)
	assert.Equal(t, &x, &y)
}
