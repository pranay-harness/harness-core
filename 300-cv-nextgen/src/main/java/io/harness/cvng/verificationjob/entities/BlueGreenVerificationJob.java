package io.harness.cvng.verificationjob.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.harness.cvng.beans.job.BlueGreenVerificationJobDTO;
import io.harness.cvng.beans.job.Sensitivity;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.core.beans.TimeRange;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(innerTypeName = "BlueGreenVerificationJobKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BlueGreenVerificationJob extends VerificationJob {
  private RuntimeParameter sensitivity;
  private Integer trafficSplitPercentage;
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.BLUE_GREEN;
  }

  @Override
  public VerificationJobDTO getVerificationJobDTO() {
    BlueGreenVerificationJobDTO blueGreenVerificationJobDTO = new BlueGreenVerificationJobDTO();
    blueGreenVerificationJobDTO.setSensitivity(getSensitivity().name());
    blueGreenVerificationJobDTO.setTrafficSplitPercentage(trafficSplitPercentage);
    populateCommonFields(blueGreenVerificationJobDTO);
    return blueGreenVerificationJobDTO;
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
  public boolean shouldDoDataCollection() {
    return true;
  }

  @Override
  protected void validateParams() {
    checkNotNull(sensitivity, generateErrorMessageFromParam(BlueGreenVerificationJobKeys.sensitivity));
    Optional.ofNullable(trafficSplitPercentage)
        .ifPresent(percentage
            -> checkState(percentage >= 0 && percentage <= 100,
                BlueGreenVerificationJobKeys.trafficSplitPercentage + " is not in appropriate range"));
  }

  @Override
  public Optional<TimeRange> getPreActivityTimeRange(Instant deploymentStartTime) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Optional<TimeRange> getPostActivityTimeRange(Instant deploymentStartTime) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public List<TimeRange> getDataCollectionTimeRanges(Instant startTime) {
    return null;
  }

  @Override
  public void fromDTO(VerificationJobDTO verificationJobDTO) {
    addCommonFileds(verificationJobDTO);
    BlueGreenVerificationJobDTO bgVerificationJobDTO = (BlueGreenVerificationJobDTO) verificationJobDTO;
    this.setSensitivity(bgVerificationJobDTO.getSensitivity(),
        VerificationJobDTO.isRuntimeParam(bgVerificationJobDTO.getSensitivity()));
    this.setTrafficSplitPercentage(bgVerificationJobDTO.getTrafficSplitPercentage());
  }

  @Override
  public void resolveJobParams(Map<String, String> runtimeParameters) {}

  @Override
  public boolean collectHostData() {
    return true;
  }
}
