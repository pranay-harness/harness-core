package io.harness.cvng.servicelevelobjective.transformer.servicelevelindicator;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.SLIMetricType;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorDTO;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorSpec;
import io.harness.cvng.servicelevelobjective.beans.slimetricspec.ThresholdSLIMetricSpec;
import io.harness.cvng.servicelevelobjective.entities.ThresholdServiceLevelIndicator;

public class ThresholdServiceLevelIndicatorTransformer
    extends ServiceLevelIndicatorTransformer<ThresholdServiceLevelIndicator, ServiceLevelIndicatorSpec> {
  @Override
  public ThresholdServiceLevelIndicator getEntity(
      ProjectParams projectParams, ServiceLevelIndicatorDTO serviceLevelIndicatorDTO) {
    ThresholdSLIMetricSpec thresholdSLIMetricSpec =
        (ThresholdSLIMetricSpec) serviceLevelIndicatorDTO.getSpec().getSpec();

    return ThresholdServiceLevelIndicator.builder()
        .accountId(projectParams.getAccountIdentifier())
        .orgIdentifier(projectParams.getOrgIdentifier())
        .projectIdentifier(projectParams.getProjectIdentifier())
        .identifier(serviceLevelIndicatorDTO.getIdentifier())
        .name(serviceLevelIndicatorDTO.getName())
        .type(serviceLevelIndicatorDTO.getType())
        .metric1(thresholdSLIMetricSpec.getMetric1())
        .build();
  }

  @Override
  protected ServiceLevelIndicatorSpec getSpec(ThresholdServiceLevelIndicator serviceLevelIndicator) {
    return ServiceLevelIndicatorSpec.builder()
        .type(SLIMetricType.THRESHOLD)
        .spec(ThresholdSLIMetricSpec.builder().metric1(serviceLevelIndicator.getMetric1()).build())
        .build();
  }
}
