// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package resolver

import (
	"context"
	"fmt"

	"github.com/wings-software/portal/commons/go/lib/expressions"
	"github.com/wings-software/portal/product/ci/addon/remote"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

var (
	evaluateJEXL = remote.EvaluateJEXL
)

// ResolveJEXLInMapValues resolves JEXL expressions present in plugin settings environment variables
func ResolveJEXLInMapValues(ctx context.Context, m map[string]string, stepID string,
	prevStepOutputs map[string]*pb.StepOutput, log *zap.SugaredLogger) (map[string]string, error) {
	var exprsToResolve []string
	for _, v := range m {
		if expressions.IsJEXL(v) {
			exprsToResolve = append(exprsToResolve, v)
		}
	}

	if len(exprsToResolve) == 0 {
		return m, nil
	}

	ret, err := evaluateJEXL(ctx, stepID, exprsToResolve, prevStepOutputs, log)
	if err != nil {
		return nil, err
	}

	resolvedMap := make(map[string]string)
	for k, v := range m {
		if resolvedVal, ok := ret[v]; ok {
			resolvedMap[k] = resolvedVal
		} else {
			resolvedMap[k] = v
		}
	}
	return resolvedMap, nil
}

// ResolveJEXLInList resolves JEXL expressions present in plugin settings environment variables
func ResolveJEXLInList(ctx context.Context, exprs []string, stepID string,
	prevStepOutputs map[string]*pb.StepOutput, log *zap.SugaredLogger) ([]string, error) {
	var exprsToResolve []string
	for _, e := range exprs {
		if expressions.IsJEXL(e) {
			exprsToResolve = append(exprsToResolve, e)
		}
	}

	if len(exprsToResolve) == 0 {
		return exprs, nil
	}

	ret, err := evaluateJEXL(ctx, stepID, exprsToResolve, prevStepOutputs, log)
	if err != nil {
		return nil, err
	}

	var resolvedExprs []string
	for _, e := range exprs {
		if resolvedVal, ok := ret[e]; ok {
			resolvedExprs = append(resolvedExprs, resolvedVal)
		} else {
			resolvedExprs = append(resolvedExprs, e)
		}
	}
	return resolvedExprs, nil
}

// ResolveJEXLInString resolves JEXL expressions present in given string
func ResolveJEXLInString(ctx context.Context, expr string, stepID string,
	prevStepOutputs map[string]*pb.StepOutput, log *zap.SugaredLogger) (string, error) {
	r, err := ResolveJEXLInList(ctx, []string{expr}, stepID, prevStepOutputs, log)
	if err != nil {
		return "", err
	}

	if len(r) == 0 {
		return "", fmt.Errorf("empty resolved expression list")
	}

	return r[0], nil
}
