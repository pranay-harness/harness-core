package io.harness.gitsync.core.service;

import javax.ws.rs.core.HttpHeaders;

public interface GitSyncTriggerService {
  String validateAndQueueWebhookRequest(String accountId, String orgIdentifier, String projectIdentifier,
      String entityToken, String yamlWebHookPayload, HttpHeaders httpHeaders);
}
