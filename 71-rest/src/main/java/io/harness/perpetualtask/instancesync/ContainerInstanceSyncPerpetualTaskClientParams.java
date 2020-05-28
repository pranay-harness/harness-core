package io.harness.perpetualtask.instancesync;

import io.harness.perpetualtask.PerpetualTaskClientParams;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContainerInstanceSyncPerpetualTaskClientParams implements PerpetualTaskClientParams {
  String appId;
  String inframappingId;
  String namespace;
  String releaseName;
  String containerSvcName;
  String containerType;
}
