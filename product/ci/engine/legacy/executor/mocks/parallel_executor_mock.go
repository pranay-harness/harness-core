// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

// Code generated by MockGen. DO NOT EDIT.
// Source: parallel_executor.go

// Package executor is a generated GoMock package.
package executor

import (
	context "context"
	gomock "github.com/golang/mock/gomock"
	output "github.com/wings-software/portal/product/ci/engine/output"
	proto "github.com/wings-software/portal/product/ci/engine/proto"
	reflect "reflect"
)

// MockParallelExecutor is a mock of ParallelExecutor interface.
type MockParallelExecutor struct {
	ctrl     *gomock.Controller
	recorder *MockParallelExecutorMockRecorder
}

// MockParallelExecutorMockRecorder is the mock recorder for MockParallelExecutor.
type MockParallelExecutorMockRecorder struct {
	mock *MockParallelExecutor
}

// NewMockParallelExecutor creates a new mock instance.
func NewMockParallelExecutor(ctrl *gomock.Controller) *MockParallelExecutor {
	mock := &MockParallelExecutor{ctrl: ctrl}
	mock.recorder = &MockParallelExecutorMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockParallelExecutor) EXPECT() *MockParallelExecutorMockRecorder {
	return m.recorder
}

// Run mocks base method.
func (m *MockParallelExecutor) Run(ctx context.Context, step *proto.ParallelStep, so output.StageOutput, accountID string) (map[string]*output.StepOutput, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Run", ctx, step, so, accountID)
	ret0, _ := ret[0].(map[string]*output.StepOutput)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// Run indicates an expected call of Run.
func (mr *MockParallelExecutorMockRecorder) Run(ctx, step, so, accountID interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Run", reflect.TypeOf((*MockParallelExecutor)(nil).Run), ctx, step, so, accountID)
}

// Cleanup mocks base method.
func (m *MockParallelExecutor) Cleanup(ctx context.Context, ps *proto.ParallelStep) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Cleanup", ctx, ps)
	ret0, _ := ret[0].(error)
	return ret0
}

// Cleanup indicates an expected call of Cleanup.
func (mr *MockParallelExecutorMockRecorder) Cleanup(ctx, ps interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Cleanup", reflect.TypeOf((*MockParallelExecutor)(nil).Cleanup), ctx, ps)
}
