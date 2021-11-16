package io.harness.cvng.servicelevelobjective.transformer.servicelevelindicator;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.SLIMetricType;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorDTO;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;

import com.google.inject.Inject;
import java.util.Map;

public class ServiceLevelIndicatorEntityAndDTOTransformer {
  @Inject private Map<SLIMetricType, ServiceLevelIndicatorTransformer> serviceLevelIndicatorTransformerMap;

  public ServiceLevelIndicator getEntity(
      ProjectParams projectParams, ServiceLevelIndicatorDTO serviceLevelIndicatorDTO) {
    ServiceLevelIndicatorTransformer serviceLevelIndicatorTransformer =
        serviceLevelIndicatorTransformerMap.get(serviceLevelIndicatorDTO.getSpec().getType());
    return serviceLevelIndicatorTransformer.getEntity(projectParams, serviceLevelIndicatorDTO);
  }

  public ServiceLevelIndicatorDTO getDto(ServiceLevelIndicator serviceLevelIndicator) {
    ServiceLevelIndicatorTransformer serviceLevelIndicatorTransformer =
        serviceLevelIndicatorTransformerMap.get(serviceLevelIndicator.getSLIMetricType());
    return serviceLevelIndicatorTransformer.getDTO(serviceLevelIndicator);
  }
}
