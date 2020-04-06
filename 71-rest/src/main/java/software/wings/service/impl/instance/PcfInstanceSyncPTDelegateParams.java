package software.wings.service.impl.instance;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;

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
