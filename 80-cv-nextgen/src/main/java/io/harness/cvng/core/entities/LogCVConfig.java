package io.harness.cvng.core.entities;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import com.google.common.base.Preconditions;

import io.harness.cvng.core.beans.TimeRange;
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
@FieldNameConstants(innerTypeName = "LogCVConfigKeys")
@EqualsAndHashCode(callSuper = true)
public abstract class LogCVConfig extends CVConfig {
  private String query;
  @Override
  public TimeRange getFirstTimeDataCollectionTimeRange() {
    TimeRange baseline = getBaseline();
    return TimeRange.builder()
        .startTime(baseline.getStartTime())
        .endTime(baseline.getStartTime().plus(5, ChronoUnit.MINUTES))
        .build();
  }

  public TimeRange getBaseline() {
    Preconditions.checkState(this.getCreatedAt() != 0, "CreatedAt needs to be set to get the baseline");
    Instant endTime = DateTimeUtils.roundDownTo5MinBoundary(Instant.ofEpochMilli(this.getCreatedAt()));
    return TimeRange.builder().endTime(endTime).startTime(endTime.minus(30, ChronoUnit.MINUTES)).build();
  }
  @Override
  public VerificationType getVerificationType() {
    return VerificationType.LOG;
  }

  @Override
  public void validate() {
    super.validate();
    checkNotNull(query, generateErrorMessageFromParam(LogCVConfigKeys.query));
  }
}
