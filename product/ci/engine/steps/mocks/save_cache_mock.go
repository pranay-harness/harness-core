// Code generated by MockGen. DO NOT EDIT.
// Source: save_cache.go

// Package steps is a generated GoMock package.
package steps

import (
	context "context"
	gomock "github.com/golang/mock/gomock"
	reflect "reflect"
)

// MockSaveCacheStep is a mock of SaveCacheStep interface.
type MockSaveCacheStep struct {
	ctrl     *gomock.Controller
	recorder *MockSaveCacheStepMockRecorder
}

// MockSaveCacheStepMockRecorder is the mock recorder for MockSaveCacheStep.
type MockSaveCacheStepMockRecorder struct {
	mock *MockSaveCacheStep
}

// NewMockSaveCacheStep creates a new mock instance.
func NewMockSaveCacheStep(ctrl *gomock.Controller) *MockSaveCacheStep {
	mock := &MockSaveCacheStep{ctrl: ctrl}
	mock.recorder = &MockSaveCacheStepMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockSaveCacheStep) EXPECT() *MockSaveCacheStepMockRecorder {
	return m.recorder
}

// Run mocks base method.
func (m *MockSaveCacheStep) Run(ctx context.Context) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Run", ctx)
	ret0, _ := ret[0].(error)
	return ret0
}

// Run indicates an expected call of Run.
func (mr *MockSaveCacheStepMockRecorder) Run(ctx interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Run", reflect.TypeOf((*MockSaveCacheStep)(nil).Run), ctx)
}
