package logs

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_Default_NewBuilder(t *testing.T) {
	b := NewBuilder()

	assert.Equal(t, b.Config.InitialFields, map[string]interface{}{})
	assert.Equal(t, b.Config.EncoderConfig.TimeKey, "ts")
}

func Test_NewBuilder_WithFields(t *testing.T) {
	b := NewBuilder().WithFields("k1", "v1", "k2", "v2").WithFields("k3", "v3")
	assert.Equal(t, map[string]interface{}{
		"k1": "v1",
		"k2": "v2",
		"k3": "v3",
	}, b.Config.InitialFields)
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
