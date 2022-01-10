// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package filesystem

import (
	"os"
	"path/filepath"

	"github.com/pkg/errors"
)

// ExpandTilde method expands the given file path to include the home directory
// if the path is prefixed with `~`. If it isn't prefixed with `~`, the path is
// returned as-is.
func ExpandTilde(path string) (string, error) {
	if len(path) == 0 {
		return path, nil
	}

	if path[0] != '~' {
		return path, nil
	}

	if len(path) > 1 && path[1] != '/' && path[1] != '\\' {
		return "", errors.New("cannot expand user-specific home dir")
	}

	dir, err := os.UserHomeDir()
	if err != nil {
		return "", errors.Wrap(err, "failed to fetch home directory")
	}

	return filepath.Join(dir, path[1:]), nil
}
