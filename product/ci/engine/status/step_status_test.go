package status

import (
	"context"
	"os"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	pb "github.com/wings-software/portal/910-delegate-task-grpc-service/src/main/proto/io/harness/task/service"
	dclient "github.com/wings-software/portal/commons/go/lib/delegate-task-grpc-service/grpc"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
)

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

func TestSendStatusErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	stepID := "step"
	accountID := "account"
	callbackToken := "token"
	taskID := "task"
	numRetries := int32(1)
	timeTaken := time.Duration(1)
	token := "foo"
	endpoint := "1.1.1.1"
	status := pb.StepExecutionStatus_SUCCESS

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
		got := SendStepStatus(ctx, stepID, "", accountID, callbackToken, taskID, numRetries, timeTaken, status, "", nil, log.Sugar())
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

func TestSendStatusClientCreateErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	stepID := "step"
	accountID := "account"
	callbackToken := "token"
	taskID := "task"
	numRetries := int32(1)
	timeTaken := time.Duration(1)
	token := "foo"
	endpoint := "1.1.1.1"
	svcID := "delegate-svc"
	status := pb.StepExecutionStatus_SUCCESS

	oldClient := newTaskServiceClient
	defer func() { newTaskServiceClient = oldClient }()
	newTaskServiceClient = func(ip string, log *zap.SugaredLogger) (dclient.TaskServiceClient, error) {
		return nil, errors.New("client create error")
	}

	testSetEnv(delegateSvcEndpointEnv, endpoint, t)
	testSetEnv(delegateSvcTokenEnv, token, t)
	testSetEnv(delegateSvcIDEnv, svcID, t)
	err := SendStepStatus(ctx, stepID, "test", accountID, callbackToken, taskID, numRetries, timeTaken, status, "", nil, log.Sugar())
	assert.NotNil(t, err)
	testUnsetEnv(delegateSvcEndpointEnv, t)
	testUnsetEnv(delegateSvcTokenEnv, t)
	testUnsetEnv(delegateSvcIDEnv, t)
}
