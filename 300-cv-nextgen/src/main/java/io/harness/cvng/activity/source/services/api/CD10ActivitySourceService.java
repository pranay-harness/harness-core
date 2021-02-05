package io.harness.cvng.activity.source.services.api;

public interface CD10ActivitySourceService {
  String getNextGenEnvIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String appId, String envId);
  String getNextGenServiceIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String appId, String serviceId);
}
