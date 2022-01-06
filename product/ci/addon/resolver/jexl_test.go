// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package resolver

import (
	"context"
	"fmt"
	"reflect"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

func TestResolveJEXLInMapValuesErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	expr := "<+ foo.bar>"
	m := map[string]string{"foo": expr}
	stepID := "foo"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldEvaluateJEXL := evaluateJEXL
	evaluateJEXL = func(ctx context.Context, stepID string, exprs []string, stageOutput map[string]*pb.StepOutput, log *zap.SugaredLogger) (
		map[string]string, error) {
		return nil, fmt.Errorf("failed to evaluate JEXL")
	}
	defer func() { evaluateJEXL = oldEvaluateJEXL }()

	_, got := ResolveJEXLInMapValues(ctx, m, stepID, nil, log.Sugar())
	assert.NotNil(t, got)
}

func TestResolveJEXLInMapValuesSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	expr := "<+ foo.bar>"
	m := map[string]string{"foo": expr, "bar": "hello"}

	ret := map[string]string{"foo": "world", "bar": "hello"}
	stepID := "foo"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldEvaluateJEXL := evaluateJEXL
	evaluateJEXL = func(ctx context.Context, stepID string, exprs []string, stageOutput map[string]*pb.StepOutput, log *zap.SugaredLogger) (
		map[string]string, error) {
		return map[string]string{expr: "world"}, nil
	}
	defer func() { evaluateJEXL = oldEvaluateJEXL }()

	out, got := ResolveJEXLInMapValues(ctx, m, stepID, nil, log.Sugar())
	assert.Nil(t, got)

	eq := reflect.DeepEqual(out, ret)
	assert.Equal(t, eq, true)
}

func TestResolveJEXLInStringErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	expr := "<+ foo.bar>"
	stepID := "foo"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldEvaluateJEXL := evaluateJEXL
	evaluateJEXL = func(ctx context.Context, stepID string, exprs []string, stageOutput map[string]*pb.StepOutput, log *zap.SugaredLogger) (
		map[string]string, error) {
		return nil, fmt.Errorf("failed to evaluate JEXL")
	}
	defer func() { evaluateJEXL = oldEvaluateJEXL }()

	_, got := ResolveJEXLInString(ctx, expr, stepID, nil, log.Sugar())
	assert.NotNil(t, got)
}

func TestResolveJEXLInStringSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	expr := "<+ foo.bar>"
	stepID := "foo"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldEvaluateJEXL := evaluateJEXL
	evaluateJEXL = func(ctx context.Context, stepID string, exprs []string, stageOutput map[string]*pb.StepOutput, log *zap.SugaredLogger) (
		map[string]string, error) {
		return map[string]string{expr: "hello world"}, nil
	}
	defer func() { evaluateJEXL = oldEvaluateJEXL }()

	out, got := ResolveJEXLInString(ctx, expr, stepID, nil, log.Sugar())
	assert.Nil(t, got)
	assert.Equal(t, out, "hello world")
}

func TestResolveJEXLInStringNoJEXLResolution(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	expr := "<+ foo.bar>"
	stepID := "foo"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldEvaluateJEXL := evaluateJEXL
	evaluateJEXL = func(ctx context.Context, stepID string, exprs []string, stageOutput map[string]*pb.StepOutput, log *zap.SugaredLogger) (
		map[string]string, error) {
		return map[string]string{}, nil
	}
	defer func() { evaluateJEXL = oldEvaluateJEXL }()

	out, got := ResolveJEXLInString(ctx, expr, stepID, nil, log.Sugar())
	assert.Nil(t, got)
	assert.Equal(t, out, expr)
}
