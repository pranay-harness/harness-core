package io.harness.cvng.servicelevelobjective.services.api;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.sli.SLIOnboardingGraphs;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorDTO;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelObjective.TimePeriod;

import java.util.List;

public interface ServiceLevelIndicatorService {
  List<String> create(ProjectParams projectParams, List<ServiceLevelIndicatorDTO> serviceLevelIndicatorDTOList,
      String serviceLevelObjectiveIdentifier, String monitoredServiceIndicator, String healthSourceIndicator);

  SLIOnboardingGraphs getOnboardingGraphs(ProjectParams projectParams, String monitoredServiceIdentifier,
      ServiceLevelIndicatorDTO serviceLevelIndicatorDTO, String tracingId);

  List<ServiceLevelIndicatorDTO> get(ProjectParams projectParams, List<String> serviceLevelIndicators);

  List<String> update(ProjectParams projectParams, List<ServiceLevelIndicatorDTO> serviceLevelIndicatorDTOList,
      String serviceLevelObjectiveIdentifier, List<String> serviceLevelIndicatorsList, String monitoredServiceIndicator,
      String healthSourceIndicator, TimePeriod timePeriod);

  void deleteByIdentifier(ProjectParams projectParams, List<String> serviceLevelIndicatorIdentifier);

  ServiceLevelIndicator get(String sliId);

  List<CVConfig> fetchCVConfigForSLI(ServiceLevelIndicator serviceLevelIndicator);

  ServiceLevelIndicator getServiceLevelIndicator(ProjectParams projectParams, String identifier);
}
