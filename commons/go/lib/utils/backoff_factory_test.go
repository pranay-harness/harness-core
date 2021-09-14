// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package utils

import (
	"testing"

	"github.com/cenkalti/backoff/v4"
	"github.com/stretchr/testify/assert"
)

func checkInstances(t *testing.T, factory BackOffFactory, same bool) {
	i1 := factory.NewBackOff()
	i2 := factory.NewBackOff()
	if same == true {
		assert.True(t, i1 == i2, "should not return brand new instances")
	} else {
		assert.True(t, i1 != i2, "should return brand new instances")
	}
}

func TestConstantBackOffFactory_NewBackOff(t *testing.T) {
	efactory := &ExponentialBackOffFactory{
		backoff.ExponentialBackOff{},
	}
	same := true
	checkInstances(t, efactory, !same)

	cfactory := &ConstantBackOffFactory{
		backoff.ConstantBackOff{},
	}
	checkInstances(t, cfactory, !same)

	sfactory := &StopBackOffFactory{
		backoff.StopBackOff{},
	}
	checkInstances(t, sfactory, same)

	zfactory := &ZeroBackOffFactory{
		backoff.ZeroBackOff{},
	}
	checkInstances(t, zfactory, same)

	ffactory := &FixedNumberBackOffFactory{
		1,
	}
	checkInstances(t, ffactory, !same)

	mfactory := &MaxRetriesBackOffFactory{
		1,
		zfactory,
	}
	checkInstances(t, mfactory, !same)
}

func TestWithMaxRetries(t *testing.T) {
	same := true
	maxRetries := uint64(3)
	zfactory := &ZeroBackOffFactory{
		backoff.ZeroBackOff{},
	}
	bo := WithMaxRetries(zfactory, maxRetries)
	checkInstances(t, bo, !same)
}
