// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package state

import (
	"context"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"testing"
)

func TestLogState_Singleton(t *testing.T) {
	o1 := LogState()
	o2 := LogState()
	assert.Equal(t, o1, o2)
}

func TestLogState_ClosePendingLogs(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	mWriter1 := logs.NopWriter()
	mWriter2 := logs.NopWriter()

	logger1, _ := logs.NewRemoteLogger(mWriter1)
	logger2, _ := logs.NewRemoteLogger(mWriter2)

	o1 := LogState()
	ch := o1.PendingLogs()
	ch <- logger1

	o1.ClosePendingLogs()

	ch <- logger2
	o1.ClosePendingLogs()

	assert.Equal(t, len(o1.PendingLogs()), 0)
}
