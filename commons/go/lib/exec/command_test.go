package exec

import (
	osExec "os/exec"
	"strings"
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

func TestOsCmd_String(t *testing.T) {
	//find out where 'echo' is so that test can be platform agnostic
	path, err := osExec.Command("which", "echo").Output()
	assert.NoError(t, err)
	pathString := strings.TrimSpace(string(path))

	c := osCommand.Command("echo", "hello", "beautiful world").String()
	assert.Equal(t, pathString+" hello beautiful world", c)
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
