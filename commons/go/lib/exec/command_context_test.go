// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package exec

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestOsCmdContext_WithEnvVars_Even(t *testing.T) {
	c := osCommandContextGraceful
	env := []string{
		"CUSTOMER_ID", "1234",
		"SESSION_ID", "id1234",
		"ENVIRONMENT", "prod",
	}

	cmd := c.CmdContext(context.Background(), "").WithEnvVars(env...).(*osCmdContext)
	assert.Contains(t, cmd.Env, "CUSTOMER_ID=1234")
	assert.Contains(t, cmd.Env, "SESSION_ID=id1234")
	assert.Contains(t, cmd.Env, "ENVIRONMENT=prod")
}

func TestOsCmdContext_WithEnvVars_Odd(t *testing.T) {
	c := osCommandContextGraceful
	env := []string{
		"CUSTOMER_ID", "1234",
		"SESSION_ID", "id1234",
		"ENVIRONMENT",
	}

	cmd := c.CmdContext(context.Background(), "").WithEnvVars(env...).(*osCmdContext)
	assert.Contains(t, cmd.Env, "CUSTOMER_ID=1234")
	assert.Contains(t, cmd.Env, "SESSION_ID=id1234")
	assert.NotContains(t, cmd.Env, "ENVIRONMENT")
}

func TestOsCmdContext_WithEnvVarsMap(t *testing.T) {
	c := osCommandContextGraceful
	env := map[string]string{"CUSTOMER_ID": "1234", "SESSION_ID": "id1234", "ENVIRONMENT": "prod"}

	cmd := c.CmdContext(context.Background(), "").WithEnvVarsMap(env).(*osCmdContext)
	assert.Contains(t, cmd.Env, "CUSTOMER_ID=1234")
	assert.Contains(t, cmd.Env, "SESSION_ID=id1234")
	assert.Contains(t, cmd.Env, "ENVIRONMENT=prod")
}

func TestOsCmdContext_Pid(t *testing.T) {
	c := osCommandContextGraceful
	assert.Equal(t, -1, c.CmdContext(context.Background(), "echo", "").Pid())
}

func TestOsCmdContext_NoErr(t *testing.T) {
	c := osCommandContextGraceful
	cmd := c.CmdContext(context.Background(), "pwd").WithDir("/tmp").WithStdout(nil).WithStderr(nil).WithCombinedOutput(nil).(*osCmdContext)
	err := cmd.Run()
	assert.NoError(t, err, "should not be error to run command with a working dir")
}

func TestOsCmdContext_ProcessState(t *testing.T) {
	c := osCommandContextGraceful
	cmd := c.CmdContext(context.Background(), "echo").(*osCmdContext)
	assert.Equal(t, nil, cmd.ProcessState())
	_ = cmd.Run()
	assert.NotEqual(t, nil, cmd.ProcessState())
}

func TestOsCmdContext_Output(t *testing.T) {
	c := osCommandContextGraceful
	buffer, err := c.CmdContext(context.Background(), "pwd").WithDir("/tmp").WithStdout(nil).WithStderr(nil).WithCombinedOutput(nil).(*osCmdContext).Output()
	assert.NoError(t, err, "should not be error to run command with a working dir")
	assert.Contains(t, string(buffer), "/tmp")

	buffer, err = c.CmdContext(context.Background(), "pwd").WithDir("/tmp").WithStdout(nil).WithStderr(nil).WithCombinedOutput(nil).(*osCmdContext).CombinedOutput()
	assert.NoError(t, err, "should not be error to run command with a working dir")
	assert.Contains(t, string(buffer), "/tmp")
}

func TestOsCmdContextWithSleep_NoErr(t *testing.T) {
	c := osCommandContextGraceful
	cmd := c.CmdContextWithSleep(context.Background(), 5, "pwd").WithDir("/tmp").WithStdout(nil).WithStderr(nil).WithCombinedOutput(nil).(*osCmdContext)
	err := cmd.Run()
	assert.NoError(t, err, "should not be error to run command with a working dir")
}
