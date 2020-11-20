package io.harness.perpetualtask.instancesync;

import io.harness.perpetualtask.PerpetualTaskClientParams;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AzureVMSSInstanceSyncPerpetualTaskClientParams implements PerpetualTaskClientParams {
  private String appId;
  private String infraMappingId;
  private String vmssId;
}
