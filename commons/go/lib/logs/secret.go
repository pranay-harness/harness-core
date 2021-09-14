// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

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
