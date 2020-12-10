package io.harness.cvng.activity.entities;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("CUSTOM")
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
