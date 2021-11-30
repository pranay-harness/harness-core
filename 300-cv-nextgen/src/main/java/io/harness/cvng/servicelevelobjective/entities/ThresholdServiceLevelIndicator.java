package io.harness.cvng.servicelevelobjective.entities;

import io.harness.cvng.servicelevelobjective.beans.SLIMetricType;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorSpec;
import io.harness.cvng.servicelevelobjective.beans.slimetricspec.ThresholdSLIMetricSpec;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@JsonTypeName("THRESHOLD")
@Data
@SuperBuilder
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "ThresholdServiceLevelIndicatorKeys")
@EqualsAndHashCode(callSuper = true)
public class ThresholdServiceLevelIndicator extends ServiceLevelIndicator {
  String metric1;

  @Override
  public SLIMetricType getSLIMetricType() {
    return SLIMetricType.THRESHOLD;
  }

  @Override
  public ServiceLevelIndicatorSpec getServiceLevelIndicatorSpec() {
    return ServiceLevelIndicatorSpec.builder()
        .type(SLIMetricType.THRESHOLD)
        .spec(ThresholdSLIMetricSpec.builder().metric1(metric1).build())
        .build();
  }

  @Override
  public List<String> getMetricNames() {
    List<String> metricForRatioSLI = new ArrayList<>();
    metricForRatioSLI.add(metric1);
    return metricForRatioSLI;
  }

  public static class ThresholdServiceLevelIndicatorUpdatableEntity
      extends ServiceLevelIndicatorUpdatableEntity<ThresholdServiceLevelIndicator, ThresholdServiceLevelIndicator> {
    @Override
    public void setUpdateOperations(UpdateOperations<ThresholdServiceLevelIndicator> updateOperations,
        ThresholdServiceLevelIndicator thresholdServiceLevelIndicator) {
      setCommonOperations(updateOperations, thresholdServiceLevelIndicator);
      updateOperations.set(ThresholdServiceLevelIndicatorKeys.metric1, thresholdServiceLevelIndicator.getMetric1());
    }
  }
}
