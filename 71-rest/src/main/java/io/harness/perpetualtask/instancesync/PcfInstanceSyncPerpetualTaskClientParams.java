package io.harness.perpetualtask.instancesync;

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
public class PcfInstanceSyncPerpetualTaskClientParams implements PerpetualTaskClientParams {
  String accountId;
  String inframappingId;
  String applicationName;
  String appId;
}
