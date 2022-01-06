// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package exec

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestOsCmd_WithEnvVars_Even(t *testing.T) {
	c := osCommand
	env := []string{
		"CUSTOMER_ID", "1234",
		"SESSION_ID", "id1234",
		"ENVIRONMENT", "prod",
	}

	cmd := c.Command("").WithEnvVars(env...).(*osCmd)
	assert.Contains(t, cmd.Env, "CUSTOMER_ID=1234")
	assert.Contains(t, cmd.Env, "SESSION_ID=id1234")
	assert.Contains(t, cmd.Env, "ENVIRONMENT=prod")
}

func TestOsCmd_WithEnvVars_Odd(t *testing.T) {
	c := osCommand
	env := []string{
		"CUSTOMER_ID", "1234",
		"SESSION_ID", "id1234",
		"ENVIRONMENT",
	}

	cmd := c.Command("").WithEnvVars(env...).(*osCmd)
	assert.Contains(t, cmd.Env, "CUSTOMER_ID=1234")
	assert.Contains(t, cmd.Env, "SESSION_ID=id1234")
	assert.NotContains(t, cmd.Env, "ENVIRONMENT")
}

func TestOsCmd_WithEnvVarsMap(t *testing.T) {
	c := osCommand
	env := map[string]string{"CUSTOMER_ID": "1234", "SESSION_ID": "id1234", "ENVIRONMENT": "prod"}

	cmd := c.Command("").WithEnvVarsMap(env).(*osCmd)
	assert.Contains(t, cmd.Env, "CUSTOMER_ID=1234")
	assert.Contains(t, cmd.Env, "SESSION_ID=id1234")
	assert.Contains(t, cmd.Env, "ENVIRONMENT=prod")
}

func TestOsCmd_Pid(t *testing.T) {
	c := osCommand
	assert.Equal(t, -1, c.Command("echo", "").Pid())
}

func TestOsCmd_NoErr(t *testing.T) {
	cmd := osCommand.Command("pwd").WithDir("/tmp").WithStdout(nil).WithStderr(nil).WithCombinedOutput(nil)
	err := cmd.Run()
	assert.NoError(t, err, "should not be error to run command with a working dir")
}

func TestOsCmd_ProcessState(t *testing.T) {
	cmd := osCommand.Command("echo")
	assert.Equal(t, nil, cmd.ProcessState())
	_ = cmd.Run()
	assert.NotEqual(t, nil, cmd.ProcessState())
}
