package io.harness.perpetualtask.instanceSync;

import io.harness.perpetualtask.PerpetualTaskClientParams;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PcfInstanceSyncPerpTaskClientParams implements PerpetualTaskClientParams {
  String inframappingId;
  String applicationName;
  String appId;
}
