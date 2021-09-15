/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.server;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.grpc.server.GrpcServerConfig;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class GitSyncServiceConfiguration {
  GrpcServerConfig grpcServerConfig;
}
