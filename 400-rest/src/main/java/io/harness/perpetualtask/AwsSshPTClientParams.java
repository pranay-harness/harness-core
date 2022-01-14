package io.harness.perpetualtask;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class AwsSshPTClientParams implements PerpetualTaskClientParams {
  String inframappingId;
  String appId;
}
