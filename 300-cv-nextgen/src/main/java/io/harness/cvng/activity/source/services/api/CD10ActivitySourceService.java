package io.harness.cvng.activity.source.services.api;

import io.harness.cvng.activity.beans.Cd10ValidateMappingParams;

public interface CD10ActivitySourceService {
  String getNextGenEnvIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String appId, String envId);
  String getNextGenServiceIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String appId, String serviceId);
  void validateMapping(Cd10ValidateMappingParams cd10ValidateMappingParams);
}
