package io.harness.ccm.communication;

import io.harness.ccm.communication.entities.CESlackWebhook;

public interface CESlackWebhookService {
  CESlackWebhook upsert(CESlackWebhook slackWebhook);
  CESlackWebhook getByAccountId(String accountId);
}
