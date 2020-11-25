package jexl

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"regexp"
	"time"

	"github.com/pkg/errors"
	pb "github.com/wings-software/portal/960-expression-service/src/main/proto/io/harness/expression/service"
	"github.com/wings-software/portal/commons/go/lib/expression-service/grpc"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/engine/output"
	"go.uber.org/zap"
	"google.golang.org/grpc/metadata"
)

const (
	jexlRegex              = `\$\{.*\}`
	delegateSvcEndpointEnv = "DELEGATE_SERVICE_ENDPOINT"
	delegateSvcTokenEnv    = "DELEGATE_SERVICE_TOKEN"
	delegateSvcIDEnv       = "DELEGATE_SERVICE_ID"
	delegateTokenKey       = "token"
	delegateSvcIDKey       = "serviceId"
	timeoutSecs            = 300 // timeout for evaluate expression grpc call
)

var (
	newExpressionEvalClient = grpc.NewExpressionEvalClient
)

// isJEXLExpression returns whether the input is a JEXL expression or not
func isJEXLExpression(input string) (bool, error) {
	return regexp.MatchString(jexlRegex, input)
}

// EvaluateJEXL evaluates list of JEXL expressions based on outputs. It does nothing if expression is not JEXL.
// Returns a map with key as JEXL expression and value as evaluated value of JEXL expression
func EvaluateJEXL(ctx context.Context, expressions []string, o output.StageOutput, log *zap.SugaredLogger) (
	map[string]string, error) {
	start := time.Now()
	arg, err := getRequestArg(expressions, o)
	if err != nil {
		log.Errorw(
			"Failed to create arguments for JEXL evaluation",
			"expressions", expressions,
			"stage_output", o,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return nil, err
	}
	if len(arg.Queries) == 0 {
		return nil, nil
	}

	response, err := sendRequest(ctx, arg, log)
	if err != nil {
		log.Errorw(
			"Failed to execute expression evaluation",
			"expressions", expressions,
			"stage_output", o,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return nil, err
	}
	result := make(map[string]string)
	for _, val := range response.GetValues() {
		if val.GetStatusCode() != pb.ExpressionValue_SUCCESS {
			log.Errorw(
				"Failed to evaluate expression",
				"expression", val.GetJexl(),
				"stage_output", o,
				"error_msg", val.GetErrorMessage(),
				"elapsed_time_ms", utils.TimeSince(start))
			return nil, errors.New(val.GetErrorMessage())
		}
		result[val.GetJexl()] = val.GetValue()
	}
	return result, nil
}

// getRequestArg returns arguments for expression service request to evaluate JEXL expression
func getRequestArg(expressions []string, o output.StageOutput) (*pb.ExpressionRequest, error) {
	data, err := json.Marshal(o)
	if err != nil {
		return nil, errors.Wrap(err, "failed to marshal stage output")
	}
	jsonStr := string(data)

	var queries []*pb.ExpressionQuery
	for _, expression := range expressions {
		if isJEXL, err := isJEXLExpression(expression); err != nil {
			return nil, err
		} else if !isJEXL {
			continue
		}

		queries = append(queries, &pb.ExpressionQuery{
			Jexl:        expression,
			JsonContext: jsonStr,
		})
	}
	req := &pb.ExpressionRequest{
		Queries: queries,
	}
	return req, nil
}

// sendRequest sends the request to expression service request to evaluate JEXL expressions
func sendRequest(ctx context.Context, request *pb.ExpressionRequest, log *zap.SugaredLogger) (
	*pb.ExpressionResponse, error) {
	ctx, cancel := context.WithTimeout(ctx, time.Second*time.Duration(timeoutSecs))
	defer cancel()

	endpoint, ok := os.LookupEnv(delegateSvcEndpointEnv)
	if !ok {
		return nil, errors.New(fmt.Sprintf("%s environment variable is not set", delegateSvcEndpointEnv))
	}
	token, ok := os.LookupEnv(delegateSvcTokenEnv)
	if !ok {
		return nil, errors.New(fmt.Sprintf("%s token is not set", delegateSvcTokenEnv))
	}
	serviceID, ok := os.LookupEnv(delegateSvcIDEnv)
	if !ok {
		return nil, errors.New(fmt.Sprintf("%s delegate service ID is not set", delegateSvcIDEnv))
	}

	c, err := newExpressionEvalClient(endpoint, log)
	if err != nil {
		return nil, errors.Wrap(err, "Could not create expression service client")
	}
	defer c.CloseConn()

	md := metadata.Pairs(
		delegateTokenKey, token,
		delegateSvcIDKey, serviceID,
	)
	ctx = metadata.NewOutgoingContext(ctx, md)
	response, err := c.Client().EvaluateExpression(ctx, request)
	if err != nil {
		return nil, errors.Wrap(err, "Could not evaluate JEXL expression")
	}
	return response, nil
}
