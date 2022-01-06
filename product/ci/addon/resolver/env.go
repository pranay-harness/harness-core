// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
