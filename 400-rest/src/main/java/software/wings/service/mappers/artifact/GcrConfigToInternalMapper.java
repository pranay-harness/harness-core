package software.wings.service.mappers.artifact;

import io.harness.artifacts.gcr.beans.GcrInternalConfig;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GcrConfigToInternalMapper {
  public GcrInternalConfig toGcpInternalConfig(String gcrHostName, String basicAuthHeader) {
    return GcrInternalConfig.builder().basicAuthHeader(basicAuthHeader).registryHostname(gcrHostName).build();
  }
}
