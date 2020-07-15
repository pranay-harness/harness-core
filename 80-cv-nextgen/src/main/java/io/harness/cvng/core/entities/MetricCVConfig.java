package io.harness.cvng.core.entities;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.harness.cvng.util.ErrorMessageUtils.generateErrorMessageFromParam;

import io.harness.cvng.beans.TimeRange;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.models.VerificationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
}
