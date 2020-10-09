package io.harness.cvng.verificationjob.entities;

import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.verificationjob.beans.HealthVerificationJobDTO;
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
@FieldNameConstants(innerTypeName = "HealthVerificationJobKeys")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HealthVerificationJob extends VerificationJob {
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.HEALTH;
  }

  @Override
  public VerificationJobDTO getVerificationJobDTO() {
    HealthVerificationJobDTO healthVerificationJobDTO = new HealthVerificationJobDTO();
    populateCommonFields(healthVerificationJobDTO);
    return healthVerificationJobDTO;
  }

  @Override
  protected void validateParams() {}

  @Override
  public Optional<TimeRange> getPreDeploymentTimeRange(Instant deploymentStartTime) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public List<TimeRange> getDataCollectionTimeRanges(Instant startTime) {
    return null;
  }

  @Override
  public void resolveJobParams(Map<String, String> runtimeParameters) {}

  @Override
  public boolean collectHostData() {
    return false;
  }
}
