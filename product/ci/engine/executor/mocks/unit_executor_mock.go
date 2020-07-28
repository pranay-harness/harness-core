// Code generated by MockGen. DO NOT EDIT.
// Source: unit_executor.go

// Package executor is a generated GoMock package.
package executor

import (
	context "context"
	gomock "github.com/golang/mock/gomock"
	proto "github.com/wings-software/portal/product/ci/engine/proto"
	reflect "reflect"
)

// MockUnitExecutor is a mock of UnitExecutor interface.
type MockUnitExecutor struct {
	ctrl     *gomock.Controller
	recorder *MockUnitExecutorMockRecorder
}

// MockUnitExecutorMockRecorder is the mock recorder for MockUnitExecutor.
type MockUnitExecutorMockRecorder struct {
	mock *MockUnitExecutor
}

// NewMockUnitExecutor creates a new mock instance.
func NewMockUnitExecutor(ctrl *gomock.Controller) *MockUnitExecutor {
	mock := &MockUnitExecutor{ctrl: ctrl}
	mock.recorder = &MockUnitExecutorMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockUnitExecutor) EXPECT() *MockUnitExecutorMockRecorder {
	return m.recorder
}

// Run mocks base method.
func (m *MockUnitExecutor) Run(ctx context.Context, step *proto.UnitStep) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Run", ctx, step)
	ret0, _ := ret[0].(error)
	return ret0
}

// Run indicates an expected call of Run.
func (mr *MockUnitExecutorMockRecorder) Run(ctx, step interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Run", reflect.TypeOf((*MockUnitExecutor)(nil).Run), ctx, step)
}
