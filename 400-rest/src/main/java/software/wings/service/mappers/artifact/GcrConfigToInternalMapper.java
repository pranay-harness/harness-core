package software.wings.service.mappers.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.gcr.beans.GcrInternalConfig;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.CDC)
@UtilityClass
public class GcrConfigToInternalMapper {
  public GcrInternalConfig toGcpInternalConfig(String gcrHostName, String basicAuthHeader) {
    return GcrInternalConfig.builder().basicAuthHeader(basicAuthHeader).registryHostname(gcrHostName).build();
  }
}
