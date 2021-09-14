// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package expressions

import "regexp"

const (
	jexlRegexPattern = `\<\+.*\>`
)

var (
	jexlRegex = regexp.MustCompile(jexlRegexPattern)
)

// IsJEXL method returns whether the input is JEXL expression or not
func IsJEXL(expr string) bool {
	return jexlRegex.MatchString(expr)
}
