/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import javax.ws.rs.core.HttpHeaders;

@OwnedBy(DX)
public interface GitSyncTriggerService {
  String validateAndQueueWebhookRequest(String accountId, String orgIdentifier, String projectIdentifier,
      String entityToken, String yamlWebHookPayload, HttpHeaders httpHeaders);
}
