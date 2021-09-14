// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package resolver

import (
	"os"
)

// ResolveEnvInString resolves environment variable in a string.
func ResolveEnvInString(v string) string {
	return os.ExpandEnv(v)
}

// ResolveEnvInMapValues resolves environment variable in map values.
func ResolveEnvInMapValues(m map[string]string) map[string]string {
	u := make(map[string]string)
	for k, v := range m {
		u[k] = os.ExpandEnv(v)
	}
	return u
}
