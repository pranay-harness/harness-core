// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
