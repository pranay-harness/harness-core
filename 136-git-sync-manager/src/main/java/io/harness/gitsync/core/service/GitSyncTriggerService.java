package io.harness.gitsync.core.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import javax.ws.rs.core.HttpHeaders;

@OwnedBy(DX)
public interface GitSyncTriggerService {
  String validateAndQueueWebhookRequest(String accountId, String orgIdentifier, String projectIdentifier,
      String entityToken, String yamlWebHookPayload, HttpHeaders httpHeaders);
}
