/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
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
