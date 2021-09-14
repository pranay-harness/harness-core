// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package resolver

import (
	"fmt"
	"os"
	"regexp"
	"strings"
)

const (
	defaultLevel    = "project"
	secretEnvPrefix = "HARNESS_"
)

var (
	secretRegex = regexp.MustCompile(`\${ngSecretManager.obtain\("([^"]*)", [^\)]*\)}`)
)

// ResolveSecretInList replaces secrets in the given list
// with the environment variable
func ResolveSecretInList(exprs []string) ([]string, error) {
	var resolvedExprs []string
	for _, expr := range exprs {
		r, err := ResolveSecretInString(expr)
		if err != nil {
			return nil, err
		}
		resolvedExprs = append(resolvedExprs, r)
	}
	return resolvedExprs, nil
}

// ResolveSecretInMapValues replaces secrets in the given map values
// with the environment variable
func ResolveSecretInMapValues(m map[string]string) (map[string]string, error) {
	resolvedMap := make(map[string]string)
	for k, v := range m {
		r, err := ResolveSecretInString(v)
		if err != nil {
			return nil, err
		}

		resolvedMap[k] = r
	}

	return resolvedMap, nil
}

// ResolveSecretInString replaces secret in the given expression with the
// environment variable
func ResolveSecretInString(expr string) (string, error) {
	resolved := expr
	matches := secretRegex.FindAllStringSubmatch(expr, -1)
	for _, v := range matches {
		if len(v) == 2 {
			env, err := getSecretEnvVal(v[1])
			if err != nil {
				return "", err
			}

			resolved = strings.Replace(resolved, v[0], env, -1)
		}
	}
	return resolved, nil
}

// getSecretEnv returns environent variable replacement for a secret
func getSecretEnv(secret string) (string, error) {
	level, secretID, err := getSecretLevelAndID(secret)
	if err != nil {
		return "", err
	}

	return fmt.Sprintf("$%s%s_%s", secretEnvPrefix, level, secretID), nil
}

func getSecretEnvVal(secret string) (string, error) {
	level, secretID, err := getSecretLevelAndID(secret)
	if err != nil {
		return "", err
	}

	secretEnv := fmt.Sprintf("$%s%s_%s", secretEnvPrefix, level, secretID)
	return os.ExpandEnv(secretEnv), nil
}

// getSecretLevelAndID returns level and ID for a secret
func getSecretLevelAndID(secret string) (string, string, error) {
	s := strings.Split(secret, ".")
	if len(s) == 1 && s[0] != "" {
		return defaultLevel, s[0], nil
	} else if len(s) == 2 && s[0] != "" && s[1] != "" {
		return s[0], s[1], nil
	} else {
		return "", "", fmt.Errorf("Invalid secret format %s", secret)
	}
}
