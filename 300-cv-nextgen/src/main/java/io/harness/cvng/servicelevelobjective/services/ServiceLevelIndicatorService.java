package io.harness.cvng.servicelevelobjective.services;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorDTO;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import java.util.List;

public interface ServiceLevelIndicatorService {
  List<String> create(ProjectParams projectParams, List<ServiceLevelIndicatorDTO> serviceLevelIndicatorDTOList,
      String serviceLevelObjectiveIdentifier, String monitoredServiceIndicator, String healthSourceIndicator);

  List<ServiceLevelIndicatorDTO> get(ProjectParams projectParams, List<String> serviceLevelIndicators);

  List<String> update(ProjectParams projectParams, List<ServiceLevelIndicatorDTO> serviceLevelIndicatorDTOList,
      String serviceLevelObjectiveIdentifier, List<String> serviceLevelIndicatorsList, String monitoredServiceIndicator,
      String healthSourceIndicator);

  void deleteByIdentifier(ProjectParams projectParams, List<String> serviceLevelIndicatorIdentifier);

  ServiceLevelIndicator get(String sliId);
}
