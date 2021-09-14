// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

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
