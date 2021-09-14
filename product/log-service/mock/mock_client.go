// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

// Code generated by MockGen. DO NOT EDIT.
// Source: client/client.go

// Package mock is a generated GoMock package.
package mock

import (
	context "context"
	gomock "github.com/golang/mock/gomock"
	client "github.com/wings-software/portal/product/log-service/client"
	stream "github.com/wings-software/portal/product/log-service/stream"
	io "io"
	reflect "reflect"
)

// MockClient is a mock of Client interface.
type MockClient struct {
	ctrl     *gomock.Controller
	recorder *MockClientMockRecorder
}

// MockClientMockRecorder is the mock recorder for MockClient.
type MockClientMockRecorder struct {
	mock *MockClient
}

// NewMockClient creates a new mock instance.
func NewMockClient(ctrl *gomock.Controller) *MockClient {
	mock := &MockClient{ctrl: ctrl}
	mock.recorder = &MockClientMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockClient) EXPECT() *MockClientMockRecorder {
	return m.recorder
}

// Upload mocks base method.
func (m *MockClient) Upload(ctx context.Context, key string, r io.Reader) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Upload", ctx, key, r)
	ret0, _ := ret[0].(error)
	return ret0
}

// Upload indicates an expected call of Upload.
func (mr *MockClientMockRecorder) Upload(ctx, key, r interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Upload", reflect.TypeOf((*MockClient)(nil).Upload), ctx, key, r)
}

// UploadLink mocks base method.
func (m *MockClient) UploadLink(ctx context.Context, key string) (*client.Link, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "UploadLink", ctx, key)
	ret0, _ := ret[0].(*client.Link)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// UploadLink indicates an expected call of UploadLink.
func (mr *MockClientMockRecorder) UploadLink(ctx, key interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "UploadLink", reflect.TypeOf((*MockClient)(nil).UploadLink), ctx, key)
}

// UploadUsingLink mocks base method.
func (m *MockClient) UploadUsingLink(ctx context.Context, link string, r io.Reader) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "UploadUsingLink", ctx, link, r)
	ret0, _ := ret[0].(error)
	return ret0
}

// UploadUsingLink indicates an expected call of UploadUsingLink.
func (mr *MockClientMockRecorder) UploadUsingLink(ctx, link, r interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "UploadUsingLink", reflect.TypeOf((*MockClient)(nil).UploadUsingLink), ctx, link, r)
}

// Download mocks base method.
func (m *MockClient) Download(ctx context.Context, key string) (io.ReadCloser, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Download", ctx, key)
	ret0, _ := ret[0].(io.ReadCloser)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// Download indicates an expected call of Download.
func (mr *MockClientMockRecorder) Download(ctx, key interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Download", reflect.TypeOf((*MockClient)(nil).Download), ctx, key)
}

// DownloadLink mocks base method.
func (m *MockClient) DownloadLink(ctx context.Context, key string) (*client.Link, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "DownloadLink", ctx, key)
	ret0, _ := ret[0].(*client.Link)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// DownloadLink indicates an expected call of DownloadLink.
func (mr *MockClientMockRecorder) DownloadLink(ctx, key interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "DownloadLink", reflect.TypeOf((*MockClient)(nil).DownloadLink), ctx, key)
}

// Open mocks base method.
func (m *MockClient) Open(ctx context.Context, key string) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Open", ctx, key)
	ret0, _ := ret[0].(error)
	return ret0
}

// Open indicates an expected call of Open.
func (mr *MockClientMockRecorder) Open(ctx, key interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Open", reflect.TypeOf((*MockClient)(nil).Open), ctx, key)
}

// Close mocks base method.
func (m *MockClient) Close(ctx context.Context, key string) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Close", ctx, key)
	ret0, _ := ret[0].(error)
	return ret0
}

// Close indicates an expected call of Close.
func (mr *MockClientMockRecorder) Close(ctx, key interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Close", reflect.TypeOf((*MockClient)(nil).Close), ctx, key)
}

// Write mocks base method.
func (m *MockClient) Write(ctx context.Context, key string, lines []*stream.Line) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Write", ctx, key, lines)
	ret0, _ := ret[0].(error)
	return ret0
}

// Write indicates an expected call of Write.
func (mr *MockClientMockRecorder) Write(ctx, key, lines interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Write", reflect.TypeOf((*MockClient)(nil).Write), ctx, key, lines)
}

// Tail mocks base method.
func (m *MockClient) Tail(ctx context.Context, key string) (<-chan string, <-chan error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Tail", ctx, key)
	ret0, _ := ret[0].(<-chan string)
	ret1, _ := ret[1].(<-chan error)
	return ret0, ret1
}

// Tail indicates an expected call of Tail.
func (mr *MockClientMockRecorder) Tail(ctx, key interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Tail", reflect.TypeOf((*MockClient)(nil).Tail), ctx, key)
}

// Info mocks base method.
func (m *MockClient) Info(ctx context.Context) (*stream.Info, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Info", ctx)
	ret0, _ := ret[0].(*stream.Info)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// Info indicates an expected call of Info.
func (mr *MockClientMockRecorder) Info(ctx interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Info", reflect.TypeOf((*MockClient)(nil).Info), ctx)
}
