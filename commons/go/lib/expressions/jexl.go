package expressions

import "regexp"

const (
	jexlRegexPattern = `\$\{.*\}`
)

var (
	jexlRegex = regexp.MustCompile(jexlRegexPattern)
)

// IsJEXL method returns whether the input is JEXL expression or not
func IsJEXL(expr string) bool {
	return jexlRegex.MatchString(expr)
}
