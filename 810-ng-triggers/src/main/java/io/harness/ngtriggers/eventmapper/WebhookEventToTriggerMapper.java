package io.harness.ngtriggers.eventmapper;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.dto.TriggerMappingRequestData;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;

@OwnedBy(PIPELINE)
public interface WebhookEventToTriggerMapper {
  WebhookEventMappingResponse mapWebhookEventToTriggers(TriggerMappingRequestData mappingRequestData);
}
