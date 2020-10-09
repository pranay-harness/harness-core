package io.harness.cvng.verificationjob.entities;

import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import com.google.common.base.Preconditions;

import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.TestVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@FieldNameConstants(innerTypeName = "TestVerificationJobKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TestVerificationJob extends VerificationJob {
  private RuntimeParameter sensitivity;
  private String baselineVerificationJobInstanceId;
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.TEST;
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
    TestVerificationJobDTO testVerificationJobDTO = new TestVerificationJobDTO();
    populateCommonFields(testVerificationJobDTO);
    testVerificationJobDTO.setSensitivity(this.sensitivity.string());
    testVerificationJobDTO.setBaselineVerificationJobInstanceId(baselineVerificationJobInstanceId);
    return testVerificationJobDTO;
  }

  @Override
  protected void validateParams() {
    Preconditions.checkNotNull(sensitivity, generateErrorMessageFromParam(TestVerificationJobKeys.sensitivity));
  }

  @Override
  public Optional<TimeRange> getPreDeploymentTimeRange(Instant deploymentStartTime) {
    return Optional.empty();
  }

  @Override
  public List<TimeRange> getDataCollectionTimeRanges(Instant startTime) {
    return getTimeRangesForDuration(startTime);
  }

  @Override
  public void resolveJobParams(Map<String, String> runtimeParameters) {}

  @Override
  public boolean collectHostData() {
    return false;
  }
}
