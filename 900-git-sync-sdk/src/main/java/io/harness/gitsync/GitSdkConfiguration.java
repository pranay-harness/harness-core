/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync;

import io.harness.ScmConnectionConfig;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;

import lombok.Data;

@Data
public class GitSdkConfiguration {
  /**
   * Grpc server config which sdk will start.
   */
  GrpcServerConfig gitSdkGrpcServerConfig;
  /**
   * client to communicate to gms server. For local it is: (can be seen in config where git sync is initialized)
   * <p>
   * core:
   * target: localhost:13001
   * authority: localhost
   * </p>ha
   */
  GrpcClientConfig gitManagerGrpcClientConfig;
  /**
   * Scm connection config.
   * <p>
   * <p>
   * scmConnectionConfig:
   * url: localhost:8091
   * </p>
   */
  ScmConnectionConfig scmConnectionConfig;
}
