// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package remote

import (
	"context"

	"github.com/wings-software/portal/product/ci/engine/consts"
	grpcclient "github.com/wings-software/portal/product/ci/engine/grpc/client"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

var (
	newEngineClient = grpcclient.NewEngineClient
)

// GetImageEntrypoint returns the entrypoint of the image.
// It calls lite engine via GRPC to fetch the entrypoint.
func GetImageEntrypoint(ctx context.Context, id, image, secret string, log *zap.SugaredLogger) ([]string, []string, error) {
	client, err := newEngineClient(consts.LiteEnginePort, log)
	if err != nil {
		log.Errorw("failed to create engine client", zap.Error(err))
		return nil, nil, err
	}
	defer client.CloseConn()

	request := &pb.GetImageEntrypointRequest{
		Id:     id,
		Image:  image,
		Secret: secret,
	}
	response, err := client.Client().GetImageEntrypoint(ctx, request)
	if err != nil {
		log.Errorw("failed to get image entrypoint", zap.Error(err))
		return nil, nil, err
	}

	return response.GetEntrypoint(), response.GetArgs(), nil
}
