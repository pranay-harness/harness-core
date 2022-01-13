// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

// This code has been adapted from Drone
package logs

import (
	"strings"
)

const (
	maskedString = "**************" // Using same masked string as CD
)

// replacer wraps a stream writer with a replacer
type replacer struct {
	w StreamWriter
	r *strings.Replacer
}

// NewReplacer wraps a stream writer with secret masking.
func NewReplacer(w StreamWriter, secrets []Secret) StreamWriter {
	var oldnew []string
	for _, secret := range secrets {
		v := secret.GetValue()
		if len(v) == 0 || secret.IsMasked() == false {
			continue
		}

		for _, part := range strings.Split(v, "\n") {
			part = strings.TrimSpace(part)

			// avoid masking small strings
			if len(part) <= 2 {
				continue
			}

			oldnew = append(oldnew, part)
			oldnew = append(oldnew, maskedString)
		}
	}
	if len(oldnew) == 0 {
		return w
	}
	return &replacer{
		w: w,
		r: strings.NewReplacer(oldnew...),
	}
}

// Write writes p to the base writer. The method scans for any
// sensitive data in p and masks before writing.
func (r *replacer) Write(p []byte) (n int, err error) {
	_, err = r.w.Write([]byte(r.r.Replace(string(p))))
	return len(p), err
}

func (r *replacer) Open() error {
	return r.w.Open()
}

func (r *replacer) Start() error {
	return r.w.Start()
}

func (r *replacer) Close() error {
	return r.w.Close()
}

func (r *replacer) Error() error {
	return r.w.Error()
}
