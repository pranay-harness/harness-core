package io.harness.ngtriggers.beans.entity.metadata;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("inline")
public class CustomWebhookInlineAuthToken implements AuthTokenSpec {
  String value;
}
