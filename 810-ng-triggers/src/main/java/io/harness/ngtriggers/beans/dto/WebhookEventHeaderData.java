package io.harness.ngtriggers.beans.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WebhookEventHeaderData {
  String sourceKey;
  List<String> sourceKeyVal;
  boolean dataFound;
}
