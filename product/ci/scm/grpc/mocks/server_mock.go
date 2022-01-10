// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

// Code generated by MockGen. DO NOT EDIT.
// Source: server.go

// Package grpc is a generated GoMock package.
package grpc

import (
	gomock "github.com/golang/mock/gomock"
	reflect "reflect"
)

// MockSCMServer is a mock of SCMServer interface.
type MockSCMServer struct {
	ctrl     *gomock.Controller
	recorder *MockSCMServerMockRecorder
}

// MockSCMServerMockRecorder is the mock recorder for MockSCMServer.
type MockSCMServerMockRecorder struct {
	mock *MockSCMServer
}

// NewMockSCMServer creates a new mock instance.
func NewMockSCMServer(ctrl *gomock.Controller) *MockSCMServer {
	mock := &MockSCMServer{ctrl: ctrl}
	mock.recorder = &MockSCMServerMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockSCMServer) EXPECT() *MockSCMServerMockRecorder {
	return m.recorder
}

// Start mocks base method.
func (m *MockSCMServer) Start() {
	m.ctrl.T.Helper()
	m.ctrl.Call(m, "Start")
}

// Start indicates an expected call of Start.
func (mr *MockSCMServerMockRecorder) Start() *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Start", reflect.TypeOf((*MockSCMServer)(nil).Start))
}

// Stop mocks base method.
func (m *MockSCMServer) Stop() {
	m.ctrl.T.Helper()
	m.ctrl.Call(m, "Stop")
}

// Stop indicates an expected call of Stop.
func (mr *MockSCMServerMockRecorder) Stop() *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Stop", reflect.TypeOf((*MockSCMServer)(nil).Stop))
}
