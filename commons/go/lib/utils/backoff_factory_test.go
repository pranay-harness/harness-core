// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
