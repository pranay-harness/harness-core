package io.harness.cvng.servicelevelobjective.services.api;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.entities.SLOHealthIndicator;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import java.util.List;

public interface SLOHealthIndicatorService {
  List<SLOHealthIndicator> getFromMonitoredServiceIdentifiers(
      ProjectParams projectParams, List<String> monitoredServiceIdentifiers);
  SLOHealthIndicator get(ProjectParams projectParams, String serviceLevelObjectiveIdentifier);
  void upsert(ServiceLevelIndicator serviceLevelIndicator);
}