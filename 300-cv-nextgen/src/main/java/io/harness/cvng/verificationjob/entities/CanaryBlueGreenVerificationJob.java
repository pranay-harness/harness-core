package io.harness.cvng.verificationjob.entities;

import static io.harness.cvng.CVConstants.RUNTIME_PARAM_STRING;
import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;
import static io.harness.cvng.verificationjob.CVVerificationJobConstants.SENSITIVITY_KEY;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.harness.cvng.beans.job.Sensitivity;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.core.beans.TimeRange;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Data
@FieldNameConstants(innerTypeName = "DeploymentVerificationJobKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public abstract class CanaryBlueGreenVerificationJob extends VerificationJob {
  // TODO: move sensitivity to common base class.
  private RuntimeParameter sensitivity;
  private Integer trafficSplitPercentage; // TODO: make this runtime param and write migration.

  public Sensitivity getSensitivity() {
    if (sensitivity.isRuntimeParam()) {
      return null;
    }
    return Sensitivity.getEnum(sensitivity.getValue());
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
  public boolean shouldDoDataCollection() {
    return true;
  }

  public abstract VerificationJobDTO getVerificationJobDTO();

  @Override
  protected void validateParams() {
    checkNotNull(sensitivity, generateErrorMessageFromParam(DeploymentVerificationJobKeys.sensitivity));
    Optional.ofNullable(trafficSplitPercentage)
        .ifPresent(percentage
            -> checkState(percentage >= 0 && percentage <= 100,
                DeploymentVerificationJobKeys.trafficSplitPercentage + " is not in appropriate range"));
  }

  @Override
  public Optional<TimeRange> getPreActivityTimeRange(Instant deploymentStartTime) {
    return Optional.of(
        TimeRange.builder().startTime(deploymentStartTime.minus(getDuration())).endTime(deploymentStartTime).build());
  }

  @Override
  public Optional<TimeRange> getPostActivityTimeRange(Instant deploymentStartTime) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public List<TimeRange> getDataCollectionTimeRanges(Instant startTime) {
    return getTimeRangesForDuration(startTime);
  }

  public abstract void fromDTO(VerificationJobDTO verificationJobDTO);

  @Override
  public void resolveJobParams(Map<String, String> runtimeParameters) {
    runtimeParameters.keySet().forEach(key -> {
      switch (key) {
        case SENSITIVITY_KEY:
          if (sensitivity.isRuntimeParam()) {
            this.setSensitivity(runtimeParameters.get(key), false);
          }
          break;
        default:
          break;
      }
    });
  }

  @Override
  public boolean collectHostData() {
    return true;
  }

  public static void setCanaryBLueGreenDefaultJobParameters(
      CanaryBlueGreenVerificationJob canaryBlueGreenVerificationJob, String accountId, String orgIdentifier,
      String projectIdentifier) {
    canaryBlueGreenVerificationJob.setSensitivity(RUNTIME_PARAM_STRING, true);
    canaryBlueGreenVerificationJob.setTrafficSplitPercentage(Integer.valueOf(5));
    VerificationJob.setDefaultJobCommonParameters(
        canaryBlueGreenVerificationJob, accountId, orgIdentifier, projectIdentifier);
  }
}
