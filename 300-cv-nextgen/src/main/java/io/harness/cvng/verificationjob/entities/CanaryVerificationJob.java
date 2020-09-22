package io.harness.cvng.verificationjob.entities;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;
import static io.harness.cvng.verificationjob.CVVerificationJobConstants.SENSITIVITY_KEY;

import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.verificationjob.beans.CanaryVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@FieldNameConstants(innerTypeName = "CanaryVerificationJobKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CanaryVerificationJob extends VerificationJob {
  public static final Duration PRE_DEPLOYMENT_DATA_COLLECTION_DURATION = Duration.ofMinutes(15);
  // TODO: move sensitivity to common base class.
  private RuntimeParameter sensitivity;
  private Integer trafficSplitPercentage;
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.CANARY;
  }

  public Sensitivity getSensitivity() {
    if (sensitivity.isRuntimeParam()) {
      return null;
    }
    return Sensitivity.valueOf(sensitivity.getValue());
  }

  public void setSensitivity(String sensitivity, boolean isRuntimeParam) {
    this.sensitivity = sensitivity == null
        ? null
        : RuntimeParameter.builder().isRuntimeParam(isRuntimeParam).value(sensitivity).build();
  }

  public void setSensitivity(Sensitivity sensitivity) {
    this.sensitivity =
        sensitivity == null ? null : RuntimeParameter.builder().isRuntimeParam(false).value(sensitivity.name()).build();
  }

  @Override
  public VerificationJobDTO getVerificationJobDTO() {
    CanaryVerificationJobDTO canaryVerificationJobDTO = new CanaryVerificationJobDTO();
    populateCommonFields(canaryVerificationJobDTO);
    return canaryVerificationJobDTO;
  }

  @Override
  protected void validateParams() {
    checkNotNull(sensitivity, generateErrorMessageFromParam(CanaryVerificationJobKeys.sensitivity));
    Optional.ofNullable(trafficSplitPercentage)
        .ifPresent(percentage
            -> checkState(percentage >= 0 && percentage <= 100,
                CanaryVerificationJobKeys.trafficSplitPercentage + " is not in appropriate range"));
  }

  @Override
  public TimeRange getPreDeploymentTimeRange(Instant deploymentStartTime) {
    return TimeRange.builder()
        .startTime(deploymentStartTime.minus(PRE_DEPLOYMENT_DATA_COLLECTION_DURATION))
        .endTime(deploymentStartTime)
        .build();
  }

  @Override
  public List<TimeRange> getDataCollectionTimeRanges(Instant startTime) {
    return getTimeRangesForDuration(startTime);
  }

  @Override
  public void resolveJobParams(Map<String, String> runtimeParameters) {
    runtimeParameters.keySet().forEach(key -> {
      switch (key) {
        case SENSITIVITY_KEY:
          this.setSensitivity(runtimeParameters.get(key), false);
          break;
        default:
          break;
      }
    });
  }
}
