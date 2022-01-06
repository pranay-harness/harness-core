// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
