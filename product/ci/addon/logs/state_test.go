// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
