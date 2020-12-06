package io.harness.perpetualtask;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsSshPTClientParams implements PerpetualTaskClientParams {
  String inframappingId;
  String appId;
}
