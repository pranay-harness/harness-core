package io.harness.ngtriggers.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.metadata.WebhookRegistrationStatus;

@OwnedBy(PIPELINE)
public interface NGTriggerWebhookRegistrationService {
  WebhookRegistrationStatus registerWebhook(NGTriggerEntity ngTriggerEntity);
}
