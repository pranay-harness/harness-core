package io.harness.cvng.servicelevelobjective.transformer.servicelevelindicator;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.SLIMetricType;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorSpec;
import io.harness.cvng.servicelevelobjective.beans.slimetricspec.RatioSLIMetricSpec;
import io.harness.cvng.servicelevelobjective.entities.RatioServiceLevelIndicator;

public class RatioServiceLevelIndicatorTransformer
    extends ServiceLevelIndicatorTransformer<RatioServiceLevelIndicator, ServiceLevelIndicatorSpec> {
  @Override
  public RatioServiceLevelIndicator getEntity(ProjectParams projectParams,
      ServiceLevelIndicatorDTO serviceLevelIndicatorDTO, String monitoredServiceIndicator,
      String healthSourceIndicator) {
    RatioSLIMetricSpec ratioSLIMetricSpec = (RatioSLIMetricSpec) serviceLevelIndicatorDTO.getSpec().getSpec();
    return RatioServiceLevelIndicator.builder()
        .accountId(projectParams.getAccountIdentifier())
        .orgIdentifier(projectParams.getOrgIdentifier())
        .projectIdentifier(projectParams.getProjectIdentifier())
        .identifier(serviceLevelIndicatorDTO.getIdentifier())
        .sliMissingDataType(serviceLevelIndicatorDTO.getSliMissingDataType())
        .name(serviceLevelIndicatorDTO.getName())
        .type(serviceLevelIndicatorDTO.getType())
        .metric1(ratioSLIMetricSpec.getMetric1())
        .metric2(ratioSLIMetricSpec.getMetric2())
        .eventType(ratioSLIMetricSpec.getEventType())
        .thresholdValue(ratioSLIMetricSpec.getThresholdValue())
        .thresholdType(ratioSLIMetricSpec.getThresholdType())
        .monitoredServiceIdentifier(monitoredServiceIndicator)
        .healthSourceIdentifier(healthSourceIndicator)
        .build();
  }

  @Override
  protected ServiceLevelIndicatorSpec getSpec(RatioServiceLevelIndicator serviceLevelIndicator) {
    return ServiceLevelIndicatorSpec.builder()
        .type(SLIMetricType.RATIO)
        .spec(RatioSLIMetricSpec.builder()
                  .eventType(serviceLevelIndicator.getEventType())
                  .metric1(serviceLevelIndicator.getMetric1())
                  .metric2(serviceLevelIndicator.getMetric2())
                  .thresholdValue(serviceLevelIndicator.getThresholdValue())
                  .thresholdType(serviceLevelIndicator.getThresholdType())
                  .build())
        .build();
  }
}
