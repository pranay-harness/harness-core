package software.wings.service.mappers.artifact;

import io.harness.artifacts.gcr.beans.GcpInternalConfig;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GcpConfigToInternalMapper {
  public GcpInternalConfig toGcpInternalConfig(String gcrHostName, String basicAuthHeader) {
    return GcpInternalConfig.builder().basicAuthHeader(basicAuthHeader).gcrHostName(gcrHostName).build();
  }
}
