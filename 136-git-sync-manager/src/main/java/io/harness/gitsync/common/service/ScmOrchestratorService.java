/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.function.Function;

@OwnedBy(DX)
public interface ScmOrchestratorService {
  // Caller code eg:
  //    processScmRequest( c->c.listFiles(params);
  <R> R processScmRequest(Function<ScmClientFacilitatorService, R> scmRequest, String projectIdentifier,
      String orgIdentifier, String accountId);

  boolean isExecuteOnDelegate(String projectIdentifier, String orgIdentifier, String accountId);
}
