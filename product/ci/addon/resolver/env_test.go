package resolver

import (
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestResolveEnvInString(t *testing.T) {
	k, v := "FOO", "BAR"
	os.Setenv(k, v)
	defer os.Unsetenv(k)

	r := ResolveEnvInString("hello $FOO")
	assert.Equal(t, r, "hello BAR")
}

func TestResolveEnvInMapValues(t *testing.T) {
	k, v := "FOO", "BAR"
	os.Setenv(k, v)
	defer os.Unsetenv(k)

	m := make(map[string]string)
	m["foo"] = "hello $FOO"
	r := ResolveEnvInMapValues(m)
	assert.Equal(t, r["foo"], "hello BAR")
}
