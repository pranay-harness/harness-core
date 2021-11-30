package io.harness.cvng.servicelevelobjective.entities;

import io.harness.cvng.servicelevelobjective.beans.SLIMetricType;
import io.harness.cvng.servicelevelobjective.beans.ServiceLevelIndicatorSpec;
import io.harness.cvng.servicelevelobjective.beans.slimetricspec.RatioSLIMetricSpec;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@JsonTypeName("RATIO")
@Data
@SuperBuilder
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "RatioServiceLevelIndicatorKeys")
@EqualsAndHashCode(callSuper = true)
public class RatioServiceLevelIndicator extends ServiceLevelIndicator {
  String cvConfigIdentifier1;
  String eventType;
  String metric1;
  String metric2;

  @Override
  public SLIMetricType getSLIMetricType() {
    return SLIMetricType.RATIO;
  }

  @Override
  public ServiceLevelIndicatorSpec getServiceLevelIndicatorSpec() {
    return ServiceLevelIndicatorSpec.builder()
        .type(SLIMetricType.RATIO)
        .spec(RatioSLIMetricSpec.builder().eventType(eventType).metric1(metric1).metric2(metric2).build())
        .build();
  }

  @Override
  public List<String> getMetricNames() {
    List<String> metricForRatioSLI = new ArrayList<>();
    metricForRatioSLI.add(metric1);
    metricForRatioSLI.add(metric2);
    return metricForRatioSLI;
  }

  public static class RatioServiceLevelIndicatorUpdatableEntity
      extends ServiceLevelIndicatorUpdatableEntity<RatioServiceLevelIndicator, RatioServiceLevelIndicator> {
    @Override
    public void setUpdateOperations(UpdateOperations<RatioServiceLevelIndicator> updateOperations,
        RatioServiceLevelIndicator ratioServiceLevelIndicator) {
      setCommonOperations(updateOperations, ratioServiceLevelIndicator);
      updateOperations.set(RatioServiceLevelIndicatorKeys.eventType, ratioServiceLevelIndicator.getEventType())
          .set(RatioServiceLevelIndicatorKeys.metric1, ratioServiceLevelIndicator.getMetric1())
          .set(RatioServiceLevelIndicatorKeys.metric2, ratioServiceLevelIndicator.getMetric2());
    }
  }
}
