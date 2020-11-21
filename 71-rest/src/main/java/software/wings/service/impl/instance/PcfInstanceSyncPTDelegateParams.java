package software.wings.service.impl.instance;

import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PcfInstanceSyncPTDelegateParams {
  String infraMappingId;
  String applicationName;
  String orgName;
  String space;
  PcfConfig pcfConfig;
  PcfInfrastructureMapping pcfInfrastructureMapping;
}
