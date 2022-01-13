// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

// Code generated by MockGen. DO NOT EDIT.
// Source: gcs_adapter.go

// Package gcputils is a generated GoMock package.
package gcputils

import (
	storage "cloud.google.com/go/storage"
	context "context"
	gomock "github.com/golang/mock/gomock"
	reflect "reflect"
)

// MockStorageClient is a mock of StorageClient interface.
type MockStorageClient struct {
	ctrl     *gomock.Controller
	recorder *MockStorageClientMockRecorder
}

// MockStorageClientMockRecorder is the mock recorder for MockStorageClient.
type MockStorageClientMockRecorder struct {
	mock *MockStorageClient
}

// NewMockStorageClient creates a new mock instance.
func NewMockStorageClient(ctrl *gomock.Controller) *MockStorageClient {
	mock := &MockStorageClient{ctrl: ctrl}
	mock.recorder = &MockStorageClientMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockStorageClient) EXPECT() *MockStorageClientMockRecorder {
	return m.recorder
}

// Bucket mocks base method.
func (m *MockStorageClient) Bucket(name string) BucketHandle {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Bucket", name)
	ret0, _ := ret[0].(BucketHandle)
	return ret0
}

// Bucket indicates an expected call of Bucket.
func (mr *MockStorageClientMockRecorder) Bucket(name interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Bucket", reflect.TypeOf((*MockStorageClient)(nil).Bucket), name)
}

// Close mocks base method.
func (m *MockStorageClient) Close() error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Close")
	ret0, _ := ret[0].(error)
	return ret0
}

// Close indicates an expected call of Close.
func (mr *MockStorageClientMockRecorder) Close() *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Close", reflect.TypeOf((*MockStorageClient)(nil).Close))
}

// MockBucketHandle is a mock of BucketHandle interface.
type MockBucketHandle struct {
	ctrl     *gomock.Controller
	recorder *MockBucketHandleMockRecorder
}

// MockBucketHandleMockRecorder is the mock recorder for MockBucketHandle.
type MockBucketHandleMockRecorder struct {
	mock *MockBucketHandle
}

// NewMockBucketHandle creates a new mock instance.
func NewMockBucketHandle(ctrl *gomock.Controller) *MockBucketHandle {
	mock := &MockBucketHandle{ctrl: ctrl}
	mock.recorder = &MockBucketHandleMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockBucketHandle) EXPECT() *MockBucketHandleMockRecorder {
	return m.recorder
}

// Object mocks base method.
func (m *MockBucketHandle) Object(name string) ObjectHandle {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Object", name)
	ret0, _ := ret[0].(ObjectHandle)
	return ret0
}

// Object indicates an expected call of Object.
func (mr *MockBucketHandleMockRecorder) Object(name interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Object", reflect.TypeOf((*MockBucketHandle)(nil).Object), name)
}

// MockObjectHandle is a mock of ObjectHandle interface.
type MockObjectHandle struct {
	ctrl     *gomock.Controller
	recorder *MockObjectHandleMockRecorder
}

// MockObjectHandleMockRecorder is the mock recorder for MockObjectHandle.
type MockObjectHandleMockRecorder struct {
	mock *MockObjectHandle
}

// NewMockObjectHandle creates a new mock instance.
func NewMockObjectHandle(ctrl *gomock.Controller) *MockObjectHandle {
	mock := &MockObjectHandle{ctrl: ctrl}
	mock.recorder = &MockObjectHandleMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockObjectHandle) EXPECT() *MockObjectHandleMockRecorder {
	return m.recorder
}

// NewReader mocks base method.
func (m *MockObjectHandle) NewReader(arg0 context.Context) (StorageReader, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "NewReader", arg0)
	ret0, _ := ret[0].(StorageReader)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// NewReader indicates an expected call of NewReader.
func (mr *MockObjectHandleMockRecorder) NewReader(arg0 interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "NewReader", reflect.TypeOf((*MockObjectHandle)(nil).NewReader), arg0)
}

// NewWriter mocks base method.
func (m *MockObjectHandle) NewWriter(ctx context.Context) StorageWriter {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "NewWriter", ctx)
	ret0, _ := ret[0].(StorageWriter)
	return ret0
}

// NewWriter indicates an expected call of NewWriter.
func (mr *MockObjectHandleMockRecorder) NewWriter(ctx interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "NewWriter", reflect.TypeOf((*MockObjectHandle)(nil).NewWriter), ctx)
}

// Attrs mocks base method.
func (m *MockObjectHandle) Attrs(ctx context.Context) (*storage.ObjectAttrs, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Attrs", ctx)
	ret0, _ := ret[0].(*storage.ObjectAttrs)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// Attrs indicates an expected call of Attrs.
func (mr *MockObjectHandleMockRecorder) Attrs(ctx interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Attrs", reflect.TypeOf((*MockObjectHandle)(nil).Attrs), ctx)
}

// Delete mocks base method.
func (m *MockObjectHandle) Delete(ctx context.Context) error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Delete", ctx)
	ret0, _ := ret[0].(error)
	return ret0
}

// Delete indicates an expected call of Delete.
func (mr *MockObjectHandleMockRecorder) Delete(ctx interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Delete", reflect.TypeOf((*MockObjectHandle)(nil).Delete), ctx)
}

// Update mocks base method.
func (m *MockObjectHandle) Update(ctx context.Context, uattrs storage.ObjectAttrsToUpdate) (*storage.ObjectAttrs, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Update", ctx, uattrs)
	ret0, _ := ret[0].(*storage.ObjectAttrs)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// Update indicates an expected call of Update.
func (mr *MockObjectHandleMockRecorder) Update(ctx, uattrs interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Update", reflect.TypeOf((*MockObjectHandle)(nil).Update), ctx, uattrs)
}

// MockStorageReader is a mock of StorageReader interface.
type MockStorageReader struct {
	ctrl     *gomock.Controller
	recorder *MockStorageReaderMockRecorder
}

// MockStorageReaderMockRecorder is the mock recorder for MockStorageReader.
type MockStorageReaderMockRecorder struct {
	mock *MockStorageReader
}

// NewMockStorageReader creates a new mock instance.
func NewMockStorageReader(ctrl *gomock.Controller) *MockStorageReader {
	mock := &MockStorageReader{ctrl: ctrl}
	mock.recorder = &MockStorageReaderMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockStorageReader) EXPECT() *MockStorageReaderMockRecorder {
	return m.recorder
}

// Read mocks base method.
func (m *MockStorageReader) Read(p []byte) (int, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Read", p)
	ret0, _ := ret[0].(int)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// Read indicates an expected call of Read.
func (mr *MockStorageReaderMockRecorder) Read(p interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Read", reflect.TypeOf((*MockStorageReader)(nil).Read), p)
}

// Close mocks base method.
func (m *MockStorageReader) Close() error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Close")
	ret0, _ := ret[0].(error)
	return ret0
}

// Close indicates an expected call of Close.
func (mr *MockStorageReaderMockRecorder) Close() *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Close", reflect.TypeOf((*MockStorageReader)(nil).Close))
}

// MockStorageWriter is a mock of StorageWriter interface.
type MockStorageWriter struct {
	ctrl     *gomock.Controller
	recorder *MockStorageWriterMockRecorder
}

// MockStorageWriterMockRecorder is the mock recorder for MockStorageWriter.
type MockStorageWriterMockRecorder struct {
	mock *MockStorageWriter
}

// NewMockStorageWriter creates a new mock instance.
func NewMockStorageWriter(ctrl *gomock.Controller) *MockStorageWriter {
	mock := &MockStorageWriter{ctrl: ctrl}
	mock.recorder = &MockStorageWriterMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockStorageWriter) EXPECT() *MockStorageWriterMockRecorder {
	return m.recorder
}

// Write mocks base method.
func (m *MockStorageWriter) Write(p []byte) (int, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Write", p)
	ret0, _ := ret[0].(int)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// Write indicates an expected call of Write.
func (mr *MockStorageWriterMockRecorder) Write(p interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Write", reflect.TypeOf((*MockStorageWriter)(nil).Write), p)
}

// Close mocks base method.
func (m *MockStorageWriter) Close() error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Close")
	ret0, _ := ret[0].(error)
	return ret0
}

// Close indicates an expected call of Close.
func (mr *MockStorageWriterMockRecorder) Close() *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Close", reflect.TypeOf((*MockStorageWriter)(nil).Close))
}
