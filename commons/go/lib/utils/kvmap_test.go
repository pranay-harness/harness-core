// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package utils

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestKVMap_MaybeGetBool(t *testing.T) {
	kvm := KVMap{
		"exists_key_true":    true,
		"exists_key_false":   false,
		"exists_key_nonbool": 2020,
	}

	val, ok := kvm.MaybeGetBool("exists_key_true")
	assert.Equal(t, true, val)
	assert.Equal(t, true, ok)

	val, ok = kvm.MaybeGetBool("exists_key_false")
	assert.Equal(t, false, val)
	assert.Equal(t, true, ok)

	val, ok = kvm.MaybeGetBool("exists_key_nonbool")
	assert.Equal(t, false, ok)

	val, ok = kvm.MaybeGetBool("key_doesn't_exist")
	assert.Equal(t, false, ok)
}

func TestKVMap_GetBool(t *testing.T) {
	kvm := KVMap{
		"exists_key_true":  true,
		"exists_key_false": false,
	}

	val := kvm.GetBool("exists_key_true")
	assert.Equal(t, true, val)

	val = kvm.GetBool("exists_key_false")
	assert.Equal(t, false, val)

	val = kvm.GetBool("key_not_found")
	assert.Equal(t, false, val)
}
func TestKVMap_MaybeGetString(t *testing.T) {
	kvm := KVMap{
		"exists_key":           "harness",
		"exists_empty_value":   "",
		"exists_key_nonstring": 2020,
	}

	val, ok := kvm.MaybeGetString("exists_key")
	assert.Equal(t, "harness", val)
	assert.Equal(t, true, ok)

	val, ok = kvm.MaybeGetString("exists_empty_value")
	assert.Equal(t, "", val)
	assert.Equal(t, true, ok)

	val, ok = kvm.MaybeGetString("exists_key_nonstring")
	assert.Equal(t, false, ok)

	val, ok = kvm.MaybeGetString("key_doesn't_exist")
	assert.Equal(t, false, ok)
}

func TestKVMap_GetString(t *testing.T) {
	kvm := KVMap{
		"exists_key":         "harness",
		"exists_empty_value": "",
	}

	val := kvm.GetString("exists_key")
	assert.Equal(t, "harness", val)

	val = kvm.GetString("exists_empty_value")
	assert.Equal(t, "", val)

	val = kvm.GetString("key_not_found")
	assert.Equal(t, "", val)

	val = kvm.GetStringOr("key_not_found", "default_value")
	assert.Equal(t, "default_value", val)

	val = kvm.GetStringOr("exists_key", "default_value")
	assert.Equal(t, "harness", val)
}

func TestKVMap_GetKVMap(t *testing.T) {
	kvm := KVMap{
		"exists_key_nonempty": KVMap{"harness": "io"},
		"exists_nonempty_map": map[string]interface{}{"harness": "io"},
		"exists_empty_value":  KVMap{},
		"exists_nonmap":       1979,
	}

	val := kvm.GetKVMap("exists_key_nonempty")
	assert.Equal(t, KVMap{"harness": "io"}, val)

	val = kvm.GetKVMap("exists_nonempty_map")
	assert.Equal(t, KVMap(KVMap{"harness": "io"}), val)

	val = kvm.GetKVMap("exists_empty_value")
	assert.Equal(t, KVMap{}, val)

	val = kvm.GetKVMap("exists_nonmap")
	assert.Equal(t, KVMap{}, val)
}

func TestKVMap_MaybeGetKVMap(t *testing.T) {
	kvm := KVMap{
		"exists_key_nonempty": KVMap{"harness": "io"},
		"exists_nonempty_map": map[string]interface{}{"harness": "io"},
		"exists_empty_value":  KVMap{},
		"exists_nonmap":       1979,
	}

	val, ok := kvm.MaybeGetKVMap("exists_key_nonempty")
	assert.Equal(t, true, ok)
	assert.Equal(t, KVMap{"harness": "io"}, val)

	val, ok = kvm.MaybeGetKVMap("exists_nonempty_map")
	assert.Equal(t, true, ok)
	assert.Equal(t, KVMap{"harness": "io"}, val)

	val, ok = kvm.MaybeGetKVMap("exists_empty_value")
	assert.Equal(t, true, ok)
	assert.Equal(t, KVMap{}, val)

	val, ok = kvm.MaybeGetKVMap("exists_nonmap")
	assert.Equal(t, false, ok)

	val, ok = kvm.MaybeGetKVMap("key_not_found")
	assert.Equal(t, false, ok)
}

func TestKVMap_MaybeGetInterface(t *testing.T) {
	kvm := KVMap{
		"exists_nonempty_map": map[string]interface{}{"harness": "io"},
	}

	val, ok := kvm.MaybeGetInterface("exists_nonempty_map")
	assert.Equal(t, true, ok)
	assert.Equal(t, map[string]interface{}(map[string]interface{}{"harness": "io"}), val)
}

func TestKVMap_GetDownstreamKVMap(t *testing.T) {
	kvm := KVMap{
		"exists_key_nonempty": KVMap{"harness": KVMap{"2nd_level_key": "io"}},
	}

	val := kvm.GetDownstreamKVMap("exists_key_nonempty", "harness")
	assert.Equal(t, KVMap{"2nd_level_key": "io"}, val)
}
