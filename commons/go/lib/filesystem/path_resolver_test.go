// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package filesystem

import (
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestExpandTilde(t *testing.T) {
	// Empty path
	path := ""
	fpath, err := ExpandTilde(path)
	assert.Equal(t, err, nil)
	assert.Equal(t, fpath, "")

	// Unset $HOME
	path = "~/home"
	_, err = ExpandTilde(path)
	assert.NotEqual(t, err, nil)

	// Set $HOME
	os.Setenv("HOME", "/test")
	path = "~/home"
	_, err = ExpandTilde(path)
	assert.Equal(t, err, nil)
	os.Unsetenv("HOME")
}
