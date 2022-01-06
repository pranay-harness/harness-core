// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package utils

import "github.com/cenkalti/backoff/v4"

//go:generate mockgen -source backoff_factory.go -destination mocks/backoff_factory_mock.go -package mocks BackOffFactory
//go:generate mockgen -destination=mocks/backoff_mock.go -package mocks github.com/cenkalti/backoff BackOff

//BackOffFactory is an object which can create pre-configured backoff.BackOff objects
type BackOffFactory interface {
	NewBackOff() backoff.BackOff
}

//assert interface implementation
var _ BackOffFactory = &ExponentialBackOffFactory{}
var _ BackOffFactory = &ConstantBackOffFactory{}
var _ BackOffFactory = &StopBackOffFactory{}
var _ BackOffFactory = &ZeroBackOffFactory{}
var _ BackOffFactory = &FixedNumberBackOffFactory{}
var _ BackOffFactory = &MaxRetriesBackOffFactory{}

//ExponentialBackOffFactory configures an operation with exponential backoff retry
type ExponentialBackOffFactory struct {
	backoff backoff.ExponentialBackOff
}

// NewExponentialBackOffFactory returns an instance of ExponentialBackOffFactory
func NewExponentialBackOffFactory() BackOffFactory {
	return &ExponentialBackOffFactory{
		backoff: *backoff.NewExponentialBackOff(),
	}
}

//NewBackOff creates a new ExponentialBackOff
func (e *ExponentialBackOffFactory) NewBackOff() backoff.BackOff {
	bo := e.backoff
	return &bo
}

//ConstantBackOffFactory configures an operation with constant-time backoff retry
type ConstantBackOffFactory struct {
	backoff backoff.ConstantBackOff
}

//NewBackOff creates a new ConstantBackOff
func (c *ConstantBackOffFactory) NewBackOff() backoff.BackOff {
	bo := c.backoff
	return &bo
}

//StopBackOffFactory configures an operation which should not be retried
type StopBackOffFactory struct {
	backoff backoff.StopBackOff
}

//NewBackOff creates a new StopBackOff
func (s *StopBackOffFactory) NewBackOff() backoff.BackOff {
	bo := s.backoff
	return &bo
}

//ZeroBackOffFactory configures an operation which should be immediately retried, indefinitely
type ZeroBackOffFactory struct {
	backoff backoff.ZeroBackOff
}

//NewBackOff creates a new ZeroBackOff
func (z *ZeroBackOffFactory) NewBackOff() backoff.BackOff {
	bo := z.backoff
	return &bo
}

//FixedNumberBackOffFactory is a helper for unit tests that immediately retries,
//up to the given number of times
type FixedNumberBackOffFactory struct {
	maxRetries uint64
}

//NewBackOff creates a new ZeroBackOff that retries at most MaxRetries times
func (f *FixedNumberBackOffFactory) NewBackOff() backoff.BackOff {
	return backoff.WithMaxRetries(&backoff.ZeroBackOff{}, f.maxRetries)
}

type MaxRetriesBackOffFactory struct {
	maxRetries uint64
	base       BackOffFactory
}

//WithMaxRetries returns a new factory that uses the given factory as a base, but adds maximum number of retries
func WithMaxRetries(b BackOffFactory, maxRetries uint64) BackOffFactory {
	return &MaxRetriesBackOffFactory{
		maxRetries: maxRetries,
		base:       b,
	}
}

//NewBackOff creates a new maxRetry BackOff
func (m *MaxRetriesBackOffFactory) NewBackOff() backoff.BackOff {
	bo := m.base.NewBackOff()
	return backoff.WithMaxRetries(bo, m.maxRetries)
}
