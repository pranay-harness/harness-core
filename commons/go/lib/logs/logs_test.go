// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package logs

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_GetLogger(t *testing.T) {
	//make sure we don't get an error
	_, err := GetLogger("qa-2020_05_07", "testGetLogger", "task123", "customer123", "session123", "application123", false)
	assert.Nil(t, err)
}
