// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package logs

//Fields is a slice of key value pairs for log metadata
type Fields []interface{}

// NewFields returns a new fields struct with the given key value pairs added
func NewFields(kvps ...interface{}) Fields {
	return (Fields{}).Add(kvps...)
}

//Add appends all values passed in the variadic kvps arg to the end and returns the result
// if is not mutated
func (f Fields) Add(kvps ...interface{}) Fields {
	return append(f, kvps...)
}

//AddFieldIf appends the field to f if the given predicate is true and returns the result
//otherwise just returns f
func (f Fields) AddFieldIf(predicate bool, key string, value interface{}) Fields {
	if predicate {
		return append(f, key, value)
	}
	return f
}

//AddField appends the given key value pair to the end of f and returns the result
// f is not mutated
func (f Fields) AddField(key string, value interface{}) Fields {
	return append(f, key, value)
}

//AddMap appends all key value pairs in the map to the end of f and returns the result
// f is not mutated
func (f Fields) AddMap(kvps map[string]interface{}) Fields {
	var f2 = f

	for key, value := range kvps {
		f2 = append(f2, key, value)
	}
	return f2
}

//AddFields appends all fields in other to the end of f and returns the result
// f is not mutated
func (f Fields) AddFields(other Fields) Fields {
	return append(f, other...)
}
