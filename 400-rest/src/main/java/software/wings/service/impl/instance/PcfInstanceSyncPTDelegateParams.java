package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.pcf.CfInternalConfig;

import software.wings.beans.PcfInfrastructureMapping;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CDP)
public class PcfInstanceSyncPTDelegateParams {
  String infraMappingId;
  String applicationName;
  String orgName;
  String space;
  CfInternalConfig pcfConfig;
  PcfInfrastructureMapping pcfInfrastructureMapping;
}
