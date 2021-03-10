package resolver

import (
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestResolveSecretInString(t *testing.T) {
	tests := []struct {
		name        string
		expr        string
		response    string
		expectedErr bool
	}{
		{
			name:        "account secret",
			expr:        `hello ${ngSecretManager.obtain("account.secret", 1234)}`,
			response:    "hello $HARNESS_account_secret",
			expectedErr: false,
		},
		{
			name:        "org secret",
			expr:        `hello ${ngSecretManager.obtain("org.secret", 1234)}`,
			response:    "hello $HARNESS_org_secret",
			expectedErr: false,
		},
		{
			name:        "project secret",
			expr:        `hello ${ngSecretManager.obtain("secret", 1234)}`,
			response:    "hello $HARNESS_project_secret",
			expectedErr: false,
		},
		{
			name:        "secret invalid",
			expr:        `hello ${ngSecretManager.obtain(".secret", 1234)}`,
			response:    "",
			expectedErr: true,
		},
		{
			name:        "secret invalid without id",
			expr:        `hello ${ngSecretManager.obtain("org.", 1234)}`,
			response:    "",
			expectedErr: true,
		},
		{
			name:        "project secret",
			expr:        `hello ${ngSecretManager.obtain("", 1234)}`,
			response:    "",
			expectedErr: true,
		},
	}

	for _, tc := range tests {
		r, got := ResolveSecretInString(tc.expr)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}

		assert.Equal(t, r, tc.response)
	}
}

func TestResolveSecretInList(t *testing.T) {
	tests := []struct {
		name        string
		expr        []string
		response    []string
		expectedErr bool
	}{
		{
			name: "account secret",
			expr: []string{
				`hello ${ngSecretManager.obtain("account.secret", 1234)}`,
				`hello ${ngSecretManager.obtain("org.secret", 1234)}`,
				`hello ${ngSecretManager.obtain("secret", 1234)}`,
			},
			response: []string{
				"hello $HARNESS_account_secret",
				"hello $HARNESS_org_secret",
				"hello $HARNESS_project_secret",
			},
			expectedErr: false,
		},
		{
			name:        "secret invalid",
			expr:        []string{`hello ${ngSecretManager.obtain(".secret", 1234)}`},
			response:    nil,
			expectedErr: true,
		},
	}

	for _, tc := range tests {
		r, got := ResolveSecretInList(tc.expr)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}

		assert.Equal(t, r, tc.response)
	}
}

func TestResolveSecretInMapValues(t *testing.T) {
	tests := []struct {
		name        string
		expr        map[string]string
		response    map[string]string
		expectedErr bool
	}{
		{
			name: "account secret",
			expr: map[string]string{
				"foo1": `hello ${ngSecretManager.obtain("account.secret", 1234)}`,
				"foo2": `hello ${ngSecretManager.obtain("org.secret", 1234)}`,
				"foo3": `hello ${ngSecretManager.obtain("secret", 1234)}`,
			},
			response: map[string]string{
				"foo1": "hello $HARNESS_account_secret",
				"foo2": "hello $HARNESS_org_secret",
				"foo3": "hello $HARNESS_project_secret",
			},
			expectedErr: false,
		},
		{
			name: "secret invalid",
			expr: map[string]string{
				"foo": `hello ${ngSecretManager.obtain(".secret", 1234)}`},
			response:    nil,
			expectedErr: true,
		},
	}

	for _, tc := range tests {
		r, got := ResolveSecretInMapValues(tc.expr)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}

		eq := reflect.DeepEqual(r, tc.response)
		assert.Equal(t, eq, true)
	}
}
