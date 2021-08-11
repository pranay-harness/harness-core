package io.harness.ngtriggers.eventmapper.filters.dto;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.polling.contracts.PollingResponse;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PIPELINE)
public class FilterRequestData {
  String accountId;
  boolean isCustomTrigger;
  List<TriggerDetails> details;
  WebhookPayloadData webhookPayloadData;
  PollingResponse pollingResponse;
}
