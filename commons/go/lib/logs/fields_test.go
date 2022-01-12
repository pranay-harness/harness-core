// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
