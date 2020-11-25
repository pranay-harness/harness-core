package status

import (
	"context"
	"fmt"
	"os"
	"time"

	"github.com/cenkalti/backoff/v4"
	"github.com/gogo/protobuf/jsonpb"
	"github.com/golang/protobuf/ptypes"
	"github.com/pkg/errors"
	delegatepb "github.com/wings-software/portal/940-delegate-beans/src/main/proto/io/harness/delegate"
	callbackpb "github.com/wings-software/portal/22-delegate-service-beans/src/main/proto/io/harness/callback"
	delegateSvcpb "github.com/wings-software/portal/22-delegate-service-beans/src/main/proto/io/harness/delegate"
	pb "github.com/wings-software/portal/50-delegate-task-grpc-service/src/main/proto/io/harness/task/service"
	"github.com/wings-software/portal/commons/go/lib/50-delegate-task-grpc-service/grpc"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/engine/output"
	"go.uber.org/zap"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
)

const (
	delegateSvcEndpointEnv = "DELEGATE_SERVICE_ENDPOINT"
	delegateSvcTokenEnv    = "DELEGATE_SERVICE_TOKEN"
	delegateSvcIDEnv       = "DELEGATE_SERVICE_ID"
	delegateTokenKey       = "token"
	delegateSvcIDKey       = "serviceId"
	timeoutSecs            = 300 // timeout for delegate send task status rpc calls
	statusRetries          = 5
)

var (
	newTaskServiceClient = grpc.NewTaskServiceClient
)

// SendStepStatus sends the step status to delegate task service.
func SendStepStatus(ctx context.Context, stepID, accountID, callbackToken, taskID string, numRetries int32, timeTaken time.Duration,
	stepOutput *output.StepOutput, stepErr error, log *zap.SugaredLogger) error {
	start := time.Now()
	arg := getRequestArg(stepID, accountID, callbackToken, taskID, numRetries, timeTaken, stepOutput, stepErr, log)
	err := sendStatusWithRetries(ctx, arg, log)
	if err != nil {
		log.Errorw(
			"Failed to send/execute delegate task status",
			"callback_token", callbackToken,
			"step_output", stepOutput,
			"step_error", stepErr,
			"elapsed_time_ms", utils.TimeSince(start),
			"step_id", stepID,
			zap.Error(err),
		)
		return err
	}
	log.Infow("Successfully sent the step status", "step_id", stepID, "elapsed_time_ms", utils.TimeSince(start))
	return nil
}

// getRequestArg returns arguments for send status rpc
func getRequestArg(stepID, accountID, callbackToken, taskID string, numRetries int32, timeTaken time.Duration,
	stepOutput *output.StepOutput, stepErr error, log *zap.SugaredLogger) *pb.SendTaskStatusRequest {
	var stepErrMsg string
	stepStatus := pb.StepExecutionStatus_SUCCESS
	if stepErr != nil {
		stepStatus = pb.StepExecutionStatus_FAILURE
		stepErrMsg = stepErr.Error()
	}

	var stepOutputMap map[string]string
	if stepOutput != nil {
		stepOutputMap = stepOutput.Output
	}
	req := &pb.SendTaskStatusRequest{
		AccountId: &delegatepb.AccountId{
			Id: accountID,
		},
		TaskId: &delegateSvcpb.TaskId{
			Id: taskID,
		},
		CallbackToken: &callbackpb.DelegateCallbackToken{
			Token: callbackToken,
		},
		TaskStatusData: &pb.TaskStatusData{
			StatusData: &pb.TaskStatusData_StepStatus{
				StepStatus: &pb.StepStatus{
					NumRetries:          numRetries,
					TotalTimeTaken:      ptypes.DurationProto(timeTaken),
					StepExecutionStatus: stepStatus,
					ErrorMessage:        stepErrMsg,
					Output: &pb.StepStatus_StepOutput{
						StepOutput: &pb.StepMapOutput{
							Output: stepOutputMap,
						},
					},
				},
			},
		},
	}
	log.Infow("Sending step status", "step_id", stepID, "status", stepStatus.String(), "arg", msgToStr(req, log))
	return req
}

func sendStatusWithRetries(ctx context.Context, request *pb.SendTaskStatusRequest, log *zap.SugaredLogger) error {
	statusUpdater := func() error {
		start := time.Now()
		err := sendRequest(ctx, request, log)
		if err != nil {
			log.Errorw(
				"failed to send step status to delegate",
				"elapsed_time_ms", utils.TimeSince(start),
				zap.Error(err),
			)
			return err
		}
		return nil
	}
	b := utils.WithMaxRetries(utils.NewExponentialBackOffFactory(), statusRetries).NewBackOff()
	err := backoff.Retry(statusUpdater, b)
	if err != nil {
		return errors.Wrap(err, fmt.Sprintf("failed to send status"))
	}
	return nil
}

// sendRequest sends the step status to delegate service
func sendRequest(ctx context.Context, request *pb.SendTaskStatusRequest, log *zap.SugaredLogger) error {
	ctx, cancel := context.WithTimeout(ctx, time.Second*time.Duration(timeoutSecs))
	defer cancel()

	endpoint, ok := os.LookupEnv(delegateSvcEndpointEnv)
	if !ok {
		return backoff.Permanent(errors.New(fmt.Sprintf("%s environment variable is not set", delegateSvcEndpointEnv)))
	}
	token, ok := os.LookupEnv(delegateSvcTokenEnv)
	if !ok {
		return backoff.Permanent(errors.New(fmt.Sprintf("%s token is not set", delegateSvcTokenEnv)))
	}
	serviceID, ok := os.LookupEnv(delegateSvcIDEnv)
	if !ok {
		return backoff.Permanent(errors.New(fmt.Sprintf("%s delegate service ID is not set", delegateSvcIDEnv)))
	}

	c, err := newTaskServiceClient(endpoint, log)
	if err != nil {
		return backoff.Permanent(errors.Wrap(err, "Could not create delegate task service client"))
	}
	defer c.CloseConn()

	md := metadata.Pairs(
		delegateTokenKey, token,
		delegateSvcIDKey, serviceID,
	)
	ctx = metadata.NewOutgoingContext(ctx, md)
	_, err = c.Client().SendTaskStatus(ctx, request)
	if err != nil {
		if e, ok := status.FromError(err); ok {
			if e.Code() == codes.Internal {
				return err
			}
		}
		return backoff.Permanent(err)
	}
	return nil
}

func msgToStr(msg *pb.SendTaskStatusRequest, log *zap.SugaredLogger) string {
	m := jsonpb.Marshaler{}
	jsonMsg, err := m.MarshalToString(msg)
	if err != nil {
		log.Errorw("failed to convert task status request to json", zap.Error(err))
		return msg.String()
	}
	return jsonMsg
}
