// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package jexl

import (
	"context"
	"fmt"
	"os"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	pb "github.com/wings-software/portal/960-expression-service/src/main/proto/io/harness/expression/service"
	dclient "github.com/wings-software/portal/commons/go/lib/expression-service/grpc"
	dmgrpc "github.com/wings-software/portal/commons/go/lib/expression-service/grpc/mocks"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/engine/output"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

type mockClient struct {
	response *pb.ExpressionResponse
	err      error
}

func (c *mockClient) EvaluateExpression(ctx context.Context, in *pb.ExpressionRequest, opts ...grpc.CallOption) (*pb.ExpressionResponse, error) {
	return c.response, c.err
}

func testSetEnv(k, v string, t *testing.T) {
	if err := os.Setenv(k, v); err != nil {
		t.Fatalf("failed to set environment variable: %s, %s", k, v)
	}
}

func testUnsetEnv(k string, t *testing.T) {
	if err := os.Unsetenv(k); err != nil {
		t.Fatalf("failed to unset environment variable: %s", k)
	}
}

// failed to find delegate service IP
func TestEvaluateJEXLErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	stepID := "step1"
	envVar1 := "foo"
	envVal1 := "bar"
	envVar2 := "hello"
	envVal2 := "world"
	token := "foo"
	endpoint := "1.1.1.1"

	so := make(output.StageOutput)
	stepO := make(map[string]string)
	stepO[envVar1] = envVal1
	stepO[envVar2] = envVal2

	o := &output.StepOutput{}
	o.Output.Variables = stepO
	so[stepID] = o

	expr1 := fmt.Sprintf("<+%s.output.%s>", stepID, envVar1)
	expr2 := fmt.Sprintf("<+%s.output.%s>", stepID, envVar2)
	expressions := []string{expr1, expr2}

	tests := []struct {
		name        string
		expectedErr bool
		envVars     map[string]string
	}{
		{
			name:        "delegate endpoint not set",
			expectedErr: true,
			envVars:     nil,
		},
		{
			name:        "token not set",
			expectedErr: true,
			envVars: map[string]string{
				delegateSvcEndpointEnv: endpoint,
			},
		},
		{
			name:        "service id not set",
			expectedErr: true,
			envVars: map[string]string{
				delegateSvcEndpointEnv: endpoint,
				delegateSvcTokenEnv:    token,
			},
		},
	}

	for _, tc := range tests {
		if tc.envVars != nil {
			for k, v := range tc.envVars {
				if err := os.Setenv(k, v); err != nil {
					t.Fatalf("%s: failed to set environment variable: %s, %s", tc.name, k, v)
				}
			}
		}
		_, got := EvaluateJEXL(ctx, stepID, expressions, so, false, log.Sugar())
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
		if tc.envVars != nil {
			for k := range tc.envVars {
				if err := os.Unsetenv(k); err != nil {
					t.Fatalf("%s: failed to unset environment variable: %s", tc.name, k)
				}
			}
		}
	}
}

func TestEvaluateJEXLClientCreateErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	stepID := "step1"
	envVar1 := "foo"
	envVal1 := "bar"
	envVar2 := "hello"
	envVal2 := "world"
	token := "foo"
	svcID := "bar"

	expr1 := fmt.Sprintf("<+%s.output.%s>", stepID, envVar1)
	expr2 := fmt.Sprintf("<+%s.output.%s>", stepID, envVar2)
	expressions := []string{expr1, expr2}

	so := make(output.StageOutput)
	stepO := make(map[string]string)
	stepO[envVar1] = envVal1
	stepO[envVar2] = envVal2

	o := &output.StepOutput{}
	o.Output.Variables = stepO
	so[stepID] = o

	oldClient := newExpressionEvalClient
	defer func() { newExpressionEvalClient = oldClient }()
	newExpressionEvalClient = func(ip string, log *zap.SugaredLogger) (dclient.ExpressionEvalClient, error) {
		return nil, errors.New("client create error")
	}

	testSetEnv(delegateSvcEndpointEnv, "1.1.1.1", t)
	testSetEnv(delegateSvcTokenEnv, token, t)
	testSetEnv(delegateSvcIDEnv, svcID, t)
	_, err := EvaluateJEXL(ctx, stepID, expressions, so, false, log.Sugar())
	assert.NotNil(t, err)
	testUnsetEnv(delegateSvcEndpointEnv, t)
	testUnsetEnv(delegateSvcTokenEnv, t)
	testUnsetEnv(delegateSvcIDEnv, t)
}

func TestEvaluateJEXLSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	stepID := "step1"
	envVar1 := "foo"
	envVal1 := "bar"
	envVar2 := "hello"
	envVal2 := "world"
	token := "foo"
	svcID := "bar"

	expr1 := fmt.Sprintf("<+%s.output.%s>", stepID, envVar1)
	expr2 := fmt.Sprintf("<+%s.output.%s>", stepID, envVar2)
	expressions := []string{expr1, expr2}

	so := make(output.StageOutput)
	stepO := make(map[string]string)
	stepO[envVar1] = envVal1
	stepO[envVar2] = envVal2

	o := &output.StepOutput{}
	o.Output.Variables = stepO
	so[stepID] = o

	c := &mockClient{
		response: &pb.ExpressionResponse{
			Values: []*pb.ExpressionValue{
				{
					Jexl:       expr1,
					Value:      envVal1,
					StatusCode: pb.ExpressionValue_SUCCESS,
				},
				{
					Jexl:       expr2,
					Value:      envVal2,
					StatusCode: pb.ExpressionValue_SUCCESS,
				},
			},
		},
	}
	oldClient := newExpressionEvalClient
	defer func() { newExpressionEvalClient = oldClient }()
	mClient := dmgrpc.NewMockExpressionEvalClient(ctrl)
	mClient.EXPECT().CloseConn().Return(nil)
	mClient.EXPECT().Client().Return(c)
	newExpressionEvalClient = func(ip string, log *zap.SugaredLogger) (dclient.ExpressionEvalClient, error) {
		return mClient, nil
	}

	testSetEnv(delegateSvcEndpointEnv, "1.1.1.1", t)
	testSetEnv(delegateSvcTokenEnv, token, t)
	testSetEnv(delegateSvcIDEnv, svcID, t)
	ret, err := EvaluateJEXL(ctx, stepID, expressions, so, false, log.Sugar())
	assert.Nil(t, err)
	assert.Equal(t, ret[expr1], envVal1)
	assert.Equal(t, ret[expr2], envVal2)
	testUnsetEnv(delegateSvcEndpointEnv, t)
	testUnsetEnv(delegateSvcTokenEnv, t)
	testUnsetEnv(delegateSvcIDEnv, t)
}

func TestEvaluateJEXLServerErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	stepID := "step1"
	envVar1 := "foo"
	envVal1 := "bar"
	envVar2 := "hello"
	envVal2 := "world"
	token := "foo"
	svcID := "bar"

	expr1 := fmt.Sprintf("<+%s.output.%s>", stepID, envVar1)
	expr2 := fmt.Sprintf("<+%s.output.%s>", stepID, envVar2)
	expressions := []string{expr1, expr2}

	so := make(output.StageOutput)
	stepO := make(map[string]string)
	stepO[envVar1] = envVal1
	stepO[envVar2] = envVal2

	o := &output.StepOutput{}
	o.Output.Variables = stepO
	so[stepID] = o

	c := &mockClient{
		response: nil,
		err:      errors.New("server not running"),
	}
	oldClient := newExpressionEvalClient
	defer func() { newExpressionEvalClient = oldClient }()
	mClient := dmgrpc.NewMockExpressionEvalClient(ctrl)
	mClient.EXPECT().CloseConn().Return(nil)
	mClient.EXPECT().Client().Return(c)
	newExpressionEvalClient = func(ip string, log *zap.SugaredLogger) (dclient.ExpressionEvalClient, error) {
		return mClient, nil
	}

	testSetEnv(delegateSvcEndpointEnv, "1.1.1.1", t)
	testSetEnv(delegateSvcTokenEnv, token, t)
	testSetEnv(delegateSvcIDEnv, svcID, t)
	_, err := EvaluateJEXL(ctx, stepID, expressions, so, false, log.Sugar())
	assert.NotNil(t, err)
	testUnsetEnv(delegateSvcEndpointEnv, t)
	testUnsetEnv(delegateSvcTokenEnv, t)
	testUnsetEnv(delegateSvcIDEnv, t)
}

func TestEvaluateJEXLInvalidExpression(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	stepID := "step1"
	envVar1 := "foo"
	envVal1 := "bar"
	envVar2 := "hello"
	envVal2 := "world"
	token := "foo"
	svcID := "bar"

	expr1 := fmt.Sprintf("<+%s.output.%s>", stepID, envVar1)
	expr2 := fmt.Sprintf("<+%s.output.%s>", stepID, envVar2)
	expressions := []string{expr1, expr2}

	so := make(output.StageOutput)
	stepO := make(map[string]string)
	stepO[envVar1] = envVal1
	stepO[envVar2] = envVal2

	o := &output.StepOutput{}
	o.Output.Variables = stepO
	so[stepID] = o

	c := &mockClient{
		response: &pb.ExpressionResponse{
			Values: []*pb.ExpressionValue{
				{
					Jexl:       expr1,
					Value:      envVal1,
					StatusCode: pb.ExpressionValue_SUCCESS,
				},
				{
					Jexl:         expr2,
					Value:        envVal2,
					StatusCode:   pb.ExpressionValue_ERROR,
					ErrorMessage: "Invalid JEXL",
				},
			},
		},
	}
	oldClient := newExpressionEvalClient
	defer func() { newExpressionEvalClient = oldClient }()
	mClient := dmgrpc.NewMockExpressionEvalClient(ctrl)
	mClient.EXPECT().CloseConn().Return(nil)
	mClient.EXPECT().Client().Return(c)
	newExpressionEvalClient = func(ip string, log *zap.SugaredLogger) (dclient.ExpressionEvalClient, error) {
		return mClient, nil
	}

	testSetEnv(delegateSvcEndpointEnv, "1.1.1.1", t)
	testSetEnv(delegateSvcTokenEnv, token, t)
	testSetEnv(delegateSvcIDEnv, svcID, t)
	_, err := EvaluateJEXL(ctx, stepID, expressions, so, false, log.Sugar())
	assert.NotNil(t, err)
	testUnsetEnv(delegateSvcEndpointEnv, t)
	testUnsetEnv(delegateSvcTokenEnv, t)
	testUnsetEnv(delegateSvcIDEnv, t)
}
