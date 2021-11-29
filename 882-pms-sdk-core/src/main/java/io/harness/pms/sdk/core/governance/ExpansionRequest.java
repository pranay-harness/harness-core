package io.harness.pms.sdk.core.governance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.YamlField;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class ExpansionRequest {
  String fqn;
  String referenceField;
  YamlField referenceFieldValue;
}
