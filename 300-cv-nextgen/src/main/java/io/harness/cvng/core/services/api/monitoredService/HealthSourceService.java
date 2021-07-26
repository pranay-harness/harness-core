package io.harness.cvng.core.services.api.monitoredService;

import io.harness.cvng.core.beans.monitoredService.HealthSource;

import java.util.List;
import java.util.Set;

public interface HealthSourceService {
  void create(String accountId, String orgIdentifier, String projectIdentifier, String environmentRef,
      String serviceRef, String nameSpaceIdentifier, Set<HealthSource> healthSources, boolean enabled);
  void checkIfAlreadyPresent(String accountId, String orgIdentifier, String projectIdentifier,
      String nameSpaceIdentifier, Set<HealthSource> healthSources);
  Set<HealthSource> get(String accountId, String orgIdentifier, String projectIdentifier, String nameSpaceIdentifier,
      List<String> identifiers);
  void delete(String accountId, String orgIdentifier, String projectIdentifier, String nameSpaceIdentifier,
      List<String> identifiers);
  void update(String accountId, String orgIdentifier, String projectIdentifier, String environmentRef,
      String serviceRef, String nameSpaceIdentifier, Set<HealthSource> healthSource);
  static String getNameSpacedIdentifier(String nameSpace, String identifier) {
    return nameSpace + "/" + identifier;
  }
  void setHealthMonitoringFlag(String accountId, String orgIdentifier, String projectIdentifier, String namespace,
      List<String> healthSourceIdentifiers, boolean enable);
}
