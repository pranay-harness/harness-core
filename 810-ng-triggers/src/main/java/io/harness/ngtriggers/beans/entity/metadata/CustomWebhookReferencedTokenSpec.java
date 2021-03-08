package io.harness.ngtriggers.beans.entity.metadata;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("ref")
public class CustomWebhookReferencedTokenSpec implements AuthTokenSpec {
  String webhookTokenIdentifier;
}
