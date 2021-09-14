// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package logs

import (
	"context"
	"sync"
	"testing"

	gomock "github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
)

type mockWriter struct {
	err  error
	data []string
	wg   sync.WaitGroup
}

func (*mockWriter) Start() error { return nil }
func (m *mockWriter) Open() error {
	m.wg.Done()
	return m.err
}
func (*mockWriter) Close() error { return nil }
func (m *mockWriter) Write(p []byte) (int, error) {
	m.data = append(m.data, string(p))
	return len(p), nil
}
func (*mockWriter) Error() error { return nil }

func Test_GetRemoteLogger_OpenSuccess(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	mw := &mockWriter{}
	mw.wg.Add(1)

	_, err := NewRemoteLogger(mw)
	mw.wg.Wait()
	assert.Equal(t, err, nil)
}

func Test_GetRemoteLogger_OpenFailure(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	mw := &mockWriter{err: errors.New("could not open stream")}
	mw.wg.Add(1)

	_, err := NewRemoteLogger(mw)
	assert.Equal(t, err, nil)
}
