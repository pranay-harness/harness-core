package io.harness.cvng.core.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import static com.google.common.base.Preconditions.checkNotNull;

import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.models.VerificationType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "MetricCVConfigKeys")
@EqualsAndHashCode(callSuper = true)
public abstract class MetricCVConfig extends CVConfig {
  private MetricPack metricPack;
  public TimeRange getFirstTimeDataCollectionTimeRange() {
    Instant endTime = DateTimeUtils.roundDownTo5MinBoundary(Instant.ofEpochMilli(this.getCreatedAt()));
    return TimeRange.builder().startTime(endTime.minus(125, ChronoUnit.MINUTES)).endTime(endTime).build();
  }
  @Override
  public VerificationType getVerificationType() {
    return VerificationType.TIME_SERIES;
  }

  @Override
  public void validate() {
    super.validate();
    checkNotNull(metricPack, generateErrorMessageFromParam(MetricCVConfigKeys.metricPack));
  }

  @Override
  public boolean queueAnalysisForPreDeploymentTask() {
    return false;
  }
}
