// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package logs

import (
	"go.uber.org/zap"
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_Default_NewDevelopmentBuilder(t *testing.T) {
	b := NewDevelopmentBuilder()

	assert.Equal(t, map[string]interface{}{}, b.Config.InitialFields)
	assert.Equal(t, "ts", b.Config.EncoderConfig.TimeKey)
}

func Test_Default_NewBuilder(t *testing.T) {
	b := NewBuilder()

	assert.Equal(t, map[string]interface{}{}, b.Config.InitialFields)
	assert.Equal(t, "ts", b.Config.EncoderConfig.TimeKey)
}

func Test_NewBuilder_WithFields(t *testing.T) {
	b := NewBuilder().WithFields("k1", "v1", "k2", "v2").WithFields("k3", "v3")
	assert.Equal(t, map[string]interface{}{
		"k1": "v1",
		"k2": "v2",
		"k3": "v3",
	}, b.Config.InitialFields)
}

func Test_NewBuilder_Verbose(t *testing.T) {
	b := NewBuilder().Verbose(true)
	assert.Equal(t, zap.DebugLevel, b.Config.Level.Level())
}

func Test_NewBuilder_MustBuild(t *testing.T) {
	b := NewBuilder().Verbose(true)
	_ = b.MustBuild()
}

func Test_NewBuilder_WithFields_OddArgsPanics(t *testing.T) {
	defer func() {
		if r := recover(); r == nil {
			assert.FailNow(t, "WithFields should panic with odd number of arguments")
		}
	}()

	// this should panic
	NewBuilder().WithFields("key1", "value1", "key2")
}

func Test_NewBuilder_WithFields_NonStringKeyPanics(t *testing.T) {
	defer func() {
		if r := recover(); r == nil {
			assert.FailNow(t, "WithFields should panic with non string key")
		}
	}()

	// this should panic
	NewBuilder().WithFields(1, 2)
}

func Test_NewBuilder_WithDeployment(t *testing.T) {
	b := NewBuilder().WithDeployment("prod-2020-xx-xx")
	assert.Equal(t, map[string]interface{}{
		"deployment":  "prod-2020-xx-xx",
		"environment": "prod",
	}, b.Config.InitialFields)

	b = NewBuilder().WithDeployment("qa-2020-xx-xx")
	assert.Equal(t, map[string]interface{}{
		"deployment":  "qa-2020-xx-xx",
		"environment": "qa",
	}, b.Config.InitialFields)

	b = NewBuilder().WithDeployment("shiv")
	assert.Equal(t, map[string]interface{}{
		"deployment":  "shiv",
		"environment": "dev",
	}, b.Config.InitialFields)
}

func Test_ExtractEnvironment(t *testing.T) {
	assert.Equal(t, "prod", ExtractEnvironment("prod-2020-05-07"))
	assert.Equal(t, "qa", ExtractEnvironment("qa-2020-05-06"))
	assert.Equal(t, "dev", ExtractEnvironment("shiv"))
}
