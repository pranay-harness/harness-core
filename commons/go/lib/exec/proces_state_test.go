// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package exec

import (
	"github.com/stretchr/testify/assert"
	"testing"
	"time"
)

func TestNullProcessState(t *testing.T) {
	ps := NullProcessState()

	assert.Equal(t, 0, ps.ExitCode())
	assert.Equal(t, true, ps.Exited())
	assert.Equal(t, 0, ps.Pid())
	assert.Equal(t, "", ps.String())
	assert.Equal(t, true, ps.Success())
	assert.Equal(t, time.Duration(0), ps.SystemTime())
	assert.Equal(t, time.Duration(0), ps.UserTime())
	_, err := ps.SysUnix()
	assert.NoError(t, err, "should not throw error")
	_, err = ps.SysUsageUnit()
	assert.NoError(t, err, "should not throw error")

}

func TestProcessStateLogFields(t *testing.T) {
	//ps := ProcessState()
	cmd := osCommand.Command("echo")
	fields := ProcessStateLogFields(cmd.ProcessState())
	assert.Nil(t, fields, "should return nil for log fields")

	_ = cmd.Run()
	fields = ProcessStateLogFields(cmd.ProcessState())
	assert.NotNil(t, fields, "should return log fields for the given process state")
}

func TestNullProcessState_SysUnix(t *testing.T) {
	cmd := osCommand.Command("echo")
	cmd.Run()
	_, err := cmd.ProcessState().SysUnix()
	assert.NoError(t, err, "should not throw error")
}
