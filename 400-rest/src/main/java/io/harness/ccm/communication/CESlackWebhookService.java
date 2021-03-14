package io.harness.ccm.communication;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.communication.entities.CESlackWebhook;
@TargetModule(Module._490_CE_COMMONS)
public interface CESlackWebhookService {
  CESlackWebhook upsert(CESlackWebhook slackWebhook);
  CESlackWebhook getByAccountId(String accountId);
}
