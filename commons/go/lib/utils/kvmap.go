// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package utils

// KVMap represents a key:value map with string keys and arbitrary values
// It is the most generic way to unmarshal a JSON object as:
// 	var obj utils.KVMap
//	json.Unmarshall(data, &obj)
type KVMap map[string]interface{}

// Delete removes a key from the map. This is just delete(m, key)
// but it allows for changing the underlying implementation in the future
func (m KVMap) Delete(key string) {
	delete(m, key)
}

//MaybeGetBool returns the value of the indicated key and the second return value indicates
//whether the key existed and was a bool
func (m KVMap) MaybeGetBool(key string) (bool, bool) {
	val, ok := m[key].(bool)
	return val, ok
}

//GetBool returns the value of the indicated key if it exists and is a bool, otherwise returns false
func (m KVMap) GetBool(key string) bool {
	val, _ := m.MaybeGetBool(key)
	return val
}

//MaybeGetString returns the value of the indicated key and the second return value indicates
//whether the key existed and was a string
func (m KVMap) MaybeGetString(key string) (string, bool) {
	val, ok := m[key].(string)
	return val, ok
}

//GetString returns the value of the indicated key if it exists and is a string, otherwise returns false
func (m KVMap) GetString(key string) string {
	val, _ := m.MaybeGetString(key)
	return val
}

//GetStringOr returns the value of the indicated key and the second return value indicates
//whether the key existed and was a string
func (m KVMap) GetStringOr(key string, def string) string {
	val, ok := m.MaybeGetString(key)
	if !ok {
		return def
	}
	return val
}

//MaybeGetKVMap returns the value of the indicated key and the second return value indicates
//whether the key existed and was a KVMap
func (m KVMap) MaybeGetKVMap(key string) (KVMap, bool) {
	iface, ok := m[key]
	if !ok {
		return KVMap{}, false
	}
	switch val := iface.(type) {
	case map[string]interface{}:
		return val, true
	case KVMap:
		return val, true
	}
	return KVMap{}, false
}

//GetKVMap returns the value of the indicated key if it exists and is a KVMap, otherwise returns false
func (m KVMap) GetKVMap(key string) KVMap {
	val, _ := m.MaybeGetKVMap(key)
	return val
}

//MaybeGetInterface returns the value of the indicated key and the second return value indicates
//whether the key existed and was a interface{}
func (m KVMap) MaybeGetInterface(key string) (interface{}, bool) {
	val, ok := m[key].(interface{})
	return val, ok
}

//GetDownstreamKVMap gets downstream KVMap specified through multiple strings
func (m KVMap) GetDownstreamKVMap(keys ...string) KVMap {
	kv := m
	for _, key := range keys {
		kv = kv.GetKVMap(key)
	}
	return kv
}
