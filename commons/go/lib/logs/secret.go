// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package logs

// Secret is an interface that must be implemented by all secrets.
type Secret interface {
	// GetName returns the secret name.
	GetName() string

	// GetValue returns the secret value.
	GetValue() string

	// IsMasked returns true if the secret value should
	// be masked. If true the secret value is masked in
	// the logs.
	IsMasked() bool
}

type secret struct {
	name     string
	value    string
	isMasked bool
}

func NewSecret(name, value string, isMasked bool) Secret {
	return &secret{
		name:     name,
		value:    value,
		isMasked: isMasked,
	}
}

func (s *secret) GetName() string {
	return s.name
}

func (s *secret) GetValue() string {
	return s.value
}

func (s *secret) IsMasked() bool {
	return s.isMasked
}
