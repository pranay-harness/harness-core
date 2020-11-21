package io.harness.cvng.activity.entities;

import io.harness.cvng.beans.ActivityDTO;
import io.harness.cvng.beans.ActivityType;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;

import lombok.Builder;
import lombok.EqualsAndHashCode;

@Builder
@EqualsAndHashCode(callSuper = true)
public class CustomActivity extends Activity {
  @Override
  public ActivityType getType() {
    return ActivityType.CUSTOM;
  }

  @Override
  public void fromDTO(ActivityDTO activityDTO) {
    setType(ActivityType.CUSTOM);
    addCommonFileds(activityDTO);
  }

  @Override
  public void fillInVerificationJobInstanceDetails(VerificationJobInstance verificationJobInstance) {}

  @Override
  public void validateActivityParams() {}
}
