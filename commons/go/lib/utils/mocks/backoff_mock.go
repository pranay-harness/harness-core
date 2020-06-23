// Code generated by MockGen. DO NOT EDIT.
// Source: github.com/cenkalti/backoff (interfaces: BackOff)

// Package mocks is a generated GoMock package.
package mocks

import (
	gomock "github.com/golang/mock/gomock"
	reflect "reflect"
	time "time"
)

// MockBackOff is a mock of BackOff interface
type MockBackOff struct {
	ctrl     *gomock.Controller
	recorder *MockBackOffMockRecorder
}

// MockBackOffMockRecorder is the mock recorder for MockBackOff
type MockBackOffMockRecorder struct {
	mock *MockBackOff
}

// NewMockBackOff creates a new mock instance
func NewMockBackOff(ctrl *gomock.Controller) *MockBackOff {
	mock := &MockBackOff{ctrl: ctrl}
	mock.recorder = &MockBackOffMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use
func (m *MockBackOff) EXPECT() *MockBackOffMockRecorder {
	return m.recorder
}

// NextBackOff mocks base method
func (m *MockBackOff) NextBackOff() time.Duration {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "NextBackOff")
	ret0, _ := ret[0].(time.Duration)
	return ret0
}

// NextBackOff indicates an expected call of NextBackOff
func (mr *MockBackOffMockRecorder) NextBackOff() *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "NextBackOff", reflect.TypeOf((*MockBackOff)(nil).NextBackOff))
}

// Reset mocks base method
func (m *MockBackOff) Reset() {
	m.ctrl.T.Helper()
	m.ctrl.Call(m, "Reset")
}

// Reset indicates an expected call of Reset
func (mr *MockBackOffMockRecorder) Reset() *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Reset", reflect.TypeOf((*MockBackOff)(nil).Reset))
}
