package io.harness.ngtriggers.eventmapper.filters.dto;

import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FilterRequestData {
  String projectFqn;
  List<TriggerDetails> details;
  WebhookPayloadData webhookPayloadData;
}
