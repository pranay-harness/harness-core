package io.harness.cvng.servicelevelobjective.services.api;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.entities.SLOHealthIndicator;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelObjective;

import java.util.List;

public interface SLOHealthIndicatorService {
  List<SLOHealthIndicator> getByMonitoredServiceIdentifiers(
      ProjectParams projectParams, List<String> monitoredServiceIdentifiers);
  SLOHealthIndicator getBySLOIdentifier(ProjectParams projectParams, String serviceLevelObjectiveIdentifier);
  List<SLOHealthIndicator> getBySLOIdentifiers(
      ProjectParams projectParams, List<String> serviceLevelObjectiveIdentifiers);
  void upsert(ServiceLevelIndicator serviceLevelIndicator);
  void upsert(ServiceLevelObjective serviceLevelObjective);
}