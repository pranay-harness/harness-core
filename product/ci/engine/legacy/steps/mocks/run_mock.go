// Code generated by MockGen. DO NOT EDIT.
// Source: run.go

// Package steps is a generated GoMock package.
package steps

import (
	context "context"
	gomock "github.com/golang/mock/gomock"
	output "github.com/wings-software/portal/product/ci/engine/output"
	reflect "reflect"
)

// MockRunStep is a mock of RunStep interface.
type MockRunStep struct {
	ctrl     *gomock.Controller
	recorder *MockRunStepMockRecorder
}

// MockRunStepMockRecorder is the mock recorder for MockRunStep.
type MockRunStepMockRecorder struct {
	mock *MockRunStep
}

// NewMockRunStep creates a new mock instance.
func NewMockRunStep(ctrl *gomock.Controller) *MockRunStep {
	mock := &MockRunStep{ctrl: ctrl}
	mock.recorder = &MockRunStepMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockRunStep) EXPECT() *MockRunStepMockRecorder {
	return m.recorder
}

// Run mocks base method.
func (m *MockRunStep) Run(ctx context.Context) (*output.StepOutput, int32, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Run", ctx)
	ret0, _ := ret[0].(*output.StepOutput)
	ret1, _ := ret[1].(int32)
	ret2, _ := ret[2].(error)
	return ret0, ret1, ret2
}

// Run indicates an expected call of Run.
func (mr *MockRunStepMockRecorder) Run(ctx interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Run", reflect.TypeOf((*MockRunStep)(nil).Run), ctx)
}
