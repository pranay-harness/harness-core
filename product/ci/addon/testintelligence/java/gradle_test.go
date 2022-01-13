// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package java

import (
	"context"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	mexec "github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.uber.org/zap"
	"os"
	"testing"
)

func TestGetGradleCmd(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)
	fs.EXPECT().Stat("gradlew").Return(nil, nil).AnyTimes()

	runner := NewGradleRunner(log.Sugar(), fs, cmdFactory)

	t1 := types.RunnableTest{Pkg: "pkg1", Class: "cls1", Method: "m1"}
	t2 := types.RunnableTest{Pkg: "pkg2", Class: "cls2", Method: "m2"}

	tests := []struct {
		name                 string // description of test
		args                 string
		runOnlySelectedTests bool
		want                 string
		expectedErr          bool
		tests                []types.RunnableTest
	}{
		{
			name:                 "run all tests with run only selected tests as false",
			args:                 "test",
			runOnlySelectedTests: false,
			want:                 "./gradlew test -DHARNESS_JAVA_AGENT=-javaagent:/addon/bin/java-agent.jar=/test/tmp/config.ini",
			expectedErr:          false,
			tests:                []types.RunnableTest{t1, t2},
		},
		{
			name:                 "run selected tests with given test list and extra args",
			args:                 "test -Duser.timezone=US/Mountain",
			runOnlySelectedTests: true,
			want:                 "./gradlew test -Duser.timezone=US/Mountain -DHARNESS_JAVA_AGENT=-javaagent:/addon/bin/java-agent.jar=/test/tmp/config.ini --tests \"pkg1.cls1\" --tests \"pkg2.cls2\"",
			expectedErr:          false,
			tests:                []types.RunnableTest{t1, t2},
		},
		{
			name:                 "run selected tests with zero tests",
			args:                 "test -Duser.timezone=US/Mountain -Duser.locale=en/US",
			runOnlySelectedTests: true,
			want:                 "echo \"Skipping test run, received no tests to execute\"",
			expectedErr:          false,
			tests:                []types.RunnableTest{},
		},
		{
			name:                 "run selected tests with repeating test list and -Duser parameters",
			args:                 "test -Duser.timezone=US/Mountain -Duser.locale=en/US",
			runOnlySelectedTests: true,
			want:                 "./gradlew test -Duser.timezone=US/Mountain -Duser.locale=en/US -DHARNESS_JAVA_AGENT=-javaagent:/addon/bin/java-agent.jar=/test/tmp/config.ini --tests \"pkg1.cls1\" --tests \"pkg2.cls2\"",
			expectedErr:          false,
			tests:                []types.RunnableTest{t1, t2, t1, t2},
		},
		{
			name:                 "run selected tests with single test and -Duser parameters and or condition",
			args:                 "test -Duser.timezone=US/Mountain -Duser.locale=en/US || true",
			runOnlySelectedTests: true,
			want:                 "./gradlew test -Duser.timezone=US/Mountain -Duser.locale=en/US -DHARNESS_JAVA_AGENT=-javaagent:/addon/bin/java-agent.jar=/test/tmp/config.ini --tests \"pkg2.cls2\" || true",
			expectedErr:          false,
			tests:                []types.RunnableTest{t2},
		},
		{
			name:                 "run selected tests with single test and -Duser parameters and multiple or conditions",
			args:                 "test -Duser.timezone=US/Mountain -Duser.locale=en/US || true || false || other",
			runOnlySelectedTests: true,
			want:                 "./gradlew test -Duser.timezone=US/Mountain -Duser.locale=en/US -DHARNESS_JAVA_AGENT=-javaagent:/addon/bin/java-agent.jar=/test/tmp/config.ini --tests \"pkg2.cls2\" || true || false || other",
			expectedErr:          false,
			tests:                []types.RunnableTest{t2},
		},
	}

	for _, tc := range tests {
		got, err := runner.GetCmd(ctx, tc.tests, tc.args, "/test/tmp/config.ini", false, !tc.runOnlySelectedTests)
		if tc.expectedErr == (err == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
		assert.Equal(t, got, tc.want, tc.name)
	}
}

func TestGetGradleCmd_Manual(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)
	fs.EXPECT().Stat("gradlew").Return(nil, os.ErrNotExist).AnyTimes()

	runner := NewGradleRunner(log.Sugar(), fs, cmdFactory)

	tests := []struct {
		name                 string // description of test
		args                 string
		runOnlySelectedTests bool
		want                 string
		expectedErr          bool
		tests                []types.RunnableTest
	}{
		{
			name:                 "run all tests with empty test list and run only selected tests as false",
			args:                 "test -Duser.timezone=en/US",
			runOnlySelectedTests: false,
			want:                 "gradle test -Duser.timezone=en/US",
			expectedErr:          false,
			tests:                []types.RunnableTest{},
		},
		{
			name:                 "run selected tests with run only selected tests as true",
			args:                 "test -Duser.timezone=US/Mountain -Duser.locale=en/US",
			runOnlySelectedTests: true,
			want:                 "gradle test -Duser.timezone=US/Mountain -Duser.locale=en/US",
			expectedErr:          false,
			tests:                []types.RunnableTest{},
		},
	}

	for _, tc := range tests {
		got, err := runner.GetCmd(ctx, tc.tests, tc.args, "/test/tmp/config.ini", true, !tc.runOnlySelectedTests)
		if tc.expectedErr == (err == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
		assert.Equal(t, got, tc.want)
	}
}
