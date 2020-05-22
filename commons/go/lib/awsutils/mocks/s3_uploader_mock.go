// Code generated by MockGen. DO NOT EDIT.
// Source: s3_uploader.go

// Package awsutils is a generated GoMock package.
package awsutils

import (
	context "context"
	s3manager "github.com/aws/aws-sdk-go/service/s3/s3manager"
	gomock "github.com/golang/mock/gomock"
	io "io"
	reflect "reflect"
)

// MockS3UploadClient is a mock of S3UploadClient interface
type MockS3UploadClient struct {
	ctrl     *gomock.Controller
	recorder *MockS3UploadClientMockRecorder
}

// MockS3UploadClientMockRecorder is the mock recorder for MockS3UploadClient
type MockS3UploadClientMockRecorder struct {
	mock *MockS3UploadClient
}

// NewMockS3UploadClient creates a new mock instance
func NewMockS3UploadClient(ctrl *gomock.Controller) *MockS3UploadClient {
	mock := &MockS3UploadClient{ctrl: ctrl}
	mock.recorder = &MockS3UploadClientMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use
func (m *MockS3UploadClient) EXPECT() *MockS3UploadClientMockRecorder {
	return m.recorder
}

// Upload mocks base method
func (m *MockS3UploadClient) Upload(input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error) {
	m.ctrl.T.Helper()
	varargs := []interface{}{input}
	for _, a := range options {
		varargs = append(varargs, a)
	}
	ret := m.ctrl.Call(m, "Upload", varargs...)
	ret0, _ := ret[0].(*s3manager.UploadOutput)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// Upload indicates an expected call of Upload
func (mr *MockS3UploadClientMockRecorder) Upload(input interface{}, options ...interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	varargs := append([]interface{}{input}, options...)
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Upload", reflect.TypeOf((*MockS3UploadClient)(nil).Upload), varargs...)
}

// UploadWithContext mocks base method
func (m *MockS3UploadClient) UploadWithContext(ctx context.Context, input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error) {
	m.ctrl.T.Helper()
	varargs := []interface{}{ctx, input}
	for _, a := range options {
		varargs = append(varargs, a)
	}
	ret := m.ctrl.Call(m, "UploadWithContext", varargs...)
	ret0, _ := ret[0].(*s3manager.UploadOutput)
	ret1, _ := ret[1].(error)
	return ret0, ret1
}

// UploadWithContext indicates an expected call of UploadWithContext
func (mr *MockS3UploadClientMockRecorder) UploadWithContext(ctx, input interface{}, options ...interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	varargs := append([]interface{}{ctx, input}, options...)
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "UploadWithContext", reflect.TypeOf((*MockS3UploadClient)(nil).UploadWithContext), varargs...)
}

// MockS3Uploader is a mock of S3Uploader interface
type MockS3Uploader struct {
	ctrl     *gomock.Controller
	recorder *MockS3UploaderMockRecorder
}

// MockS3UploaderMockRecorder is the mock recorder for MockS3Uploader
type MockS3UploaderMockRecorder struct {
	mock *MockS3Uploader
}

// NewMockS3Uploader creates a new mock instance
func NewMockS3Uploader(ctrl *gomock.Controller) *MockS3Uploader {
	mock := &MockS3Uploader{ctrl: ctrl}
	mock.recorder = &MockS3UploaderMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use
func (m *MockS3Uploader) EXPECT() *MockS3UploaderMockRecorder {
	return m.recorder
}

// UploadReader mocks base method
func (m *MockS3Uploader) UploadReader(key string, reader io.Reader) (string, string, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "UploadReader", key, reader)
	ret0, _ := ret[0].(string)
	ret1, _ := ret[1].(string)
	ret2, _ := ret[2].(error)
	return ret0, ret1, ret2
}

// UploadReader indicates an expected call of UploadReader
func (mr *MockS3UploaderMockRecorder) UploadReader(key, reader interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "UploadReader", reflect.TypeOf((*MockS3Uploader)(nil).UploadReader), key, reader)
}

// UploadReaderWithContext mocks base method
func (m *MockS3Uploader) UploadReaderWithContext(ctx context.Context, key string, reader io.Reader) (string, string, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "UploadReaderWithContext", ctx, key, reader)
	ret0, _ := ret[0].(string)
	ret1, _ := ret[1].(string)
	ret2, _ := ret[2].(error)
	return ret0, ret1, ret2
}

// UploadReaderWithContext indicates an expected call of UploadReaderWithContext
func (mr *MockS3UploaderMockRecorder) UploadReaderWithContext(ctx, key, reader interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "UploadReaderWithContext", reflect.TypeOf((*MockS3Uploader)(nil).UploadReaderWithContext), ctx, key, reader)
}

// UploadFile mocks base method
func (m *MockS3Uploader) UploadFile(key, filename string) (string, string, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "UploadFile", key, filename)
	ret0, _ := ret[0].(string)
	ret1, _ := ret[1].(string)
	ret2, _ := ret[2].(error)
	return ret0, ret1, ret2
}

// UploadFile indicates an expected call of UploadFile
func (mr *MockS3UploaderMockRecorder) UploadFile(key, filename interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "UploadFile", reflect.TypeOf((*MockS3Uploader)(nil).UploadFile), key, filename)
}

// UploadFileWithContext mocks base method
func (m *MockS3Uploader) UploadFileWithContext(ctx context.Context, key, filename string) (string, string, error) {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "UploadFileWithContext", ctx, key, filename)
	ret0, _ := ret[0].(string)
	ret1, _ := ret[1].(string)
	ret2, _ := ret[2].(error)
	return ret0, ret1, ret2
}

// UploadFileWithContext indicates an expected call of UploadFileWithContext
func (mr *MockS3UploaderMockRecorder) UploadFileWithContext(ctx, key, filename interface{}) *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "UploadFileWithContext", reflect.TypeOf((*MockS3Uploader)(nil).UploadFileWithContext), ctx, key, filename)
}
