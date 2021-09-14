// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package logs

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func Test_Fields(t *testing.T) {
	var f Fields

	assert.Empty(t, f)

	f = f.AddField("key1", 1)
	expectedFields := []interface{}{"key1", 1}
	assert.Len(t, f, 2)
	assert.ElementsMatch(t, f, expectedFields)

	f = f.Add("key2", 2, "key3", 3)
	expectedFields = append(expectedFields, "key2", 2, "key3", 3)
	assert.Len(t, f, 6)
	assert.ElementsMatch(t, f, expectedFields)

	f = f.AddMap(map[string]interface{}{
		"key4": 4,
		"key5": 5,
	})
	expectedFields = append(expectedFields, "key4", 4, "key5", 5)
	assert.Len(t, f, 10)
	assert.ElementsMatch(t, f, expectedFields)

	other := NewFields("key6", 6, "key7", 7)
	f = f.AddFields(other)
	expectedFields = append(expectedFields, "key6", 6, "key7", 7)
	assert.Len(t, f, 14)
	assert.ElementsMatch(t, f, expectedFields)

	f = f.AddFieldIf(true, "key8", 8)
	expectedFields = append(expectedFields, "key8", 8)
	assert.Len(t, f, 16)
	assert.ElementsMatch(t, f, expectedFields)

	f = f.AddFieldIf(false, "key8", 8)
	assert.Len(t, f, 16)
	assert.ElementsMatch(t, f, expectedFields)
}
