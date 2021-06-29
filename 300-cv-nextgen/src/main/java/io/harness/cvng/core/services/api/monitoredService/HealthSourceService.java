package io.harness.cvng.core.services.api.monitoredService;

import io.harness.cvng.core.beans.monitoredService.HealthSource;

import java.util.List;
import java.util.Set;

public interface HealthSourceService {
  void create(String accountId, String orgIdentifier, String projectIdentifier, String environmentRef,
      String serviceRef, Set<HealthSource> healthSources);
  void checkIfAlreadyPresent(
      String accountId, String orgIdentifier, String projectIdentifier, Set<HealthSource> healthSources);
  Set<HealthSource> get(String accountId, String orgIdentifier, String projectIdentifier, List<String> identifiers);
  void delete(String accountId, String orgIdentifier, String projectIdentifier, List<String> identifiers);
  void update(String accountId, String orgIdentifier, String projectIdentifier, String environmentRef,
      String serviceRef, Set<HealthSource> healthSource);
}
