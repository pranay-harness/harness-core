package io.harness.cvng.analysis.beans;

import io.harness.cvng.activity.beans.ActivityVerificationResultDTO.CategoryRisk;
import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.verificationjob.beans.AdditionalInfo;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
@Value
@Builder
public class HealthAdditionalInfo extends AdditionalInfo {
  Set<CategoryRisk> preActivityRisks;
  Set<CategoryRisk> postActivityRisks;

  @Override
  public VerificationJobType getType() {
    return VerificationJobType.HEALTH;
  }
}
