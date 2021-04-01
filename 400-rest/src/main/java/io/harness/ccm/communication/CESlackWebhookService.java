package io.harness.ccm.communication;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.communication.entities.CESlackWebhook;

@OwnedBy(CE)
public interface CESlackWebhookService {
  CESlackWebhook upsert(CESlackWebhook slackWebhook);
  CESlackWebhook getByAccountId(String accountId);
}
