// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package remote

import (
	"context"

	"github.com/wings-software/portal/product/ci/engine/consts"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

// EvaluateJEXL evaluates the JEXL expression remotely.
func EvaluateJEXL(ctx context.Context, stepID string, exprs []string,
	stepOutputs map[string]*pb.StepOutput, log *zap.SugaredLogger) (
	map[string]string, error) {

	client, err := newEngineClient(consts.LiteEnginePort, log)
	if err != nil {
		log.Errorw("failed to create engine client", zap.Error(err))
		return nil, err
	}
	defer client.CloseConn()

	request := &pb.EvaluateJEXLRequest{
		StepId:      stepID,
		Expressions: exprs,
		StepOutputs: stepOutputs,
	}
	response, err := client.Client().EvaluateJEXL(ctx, request)
	if err != nil {
		log.Errorw("failed to evaluate jexl expressions", zap.Error(err))
		return nil, err
	}

	return response.GetEvaluatedExpressions(), nil
}
