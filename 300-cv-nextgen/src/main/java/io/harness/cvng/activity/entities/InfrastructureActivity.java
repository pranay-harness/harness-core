package io.harness.cvng.activity.entities;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@JsonTypeName("INFRASTRUCTURE")
public class InfrastructureActivity extends Activity {
  @Override
  public ActivityType getType() {
    return ActivityType.INFRASTRUCTURE;
  }

  @Override
  public void fromDTO(ActivityDTO activityDTO) {
    addCommonFileds(activityDTO);
  }

  @Override
  public void fillInVerificationJobInstanceDetails(VerificationJobInstance verificationJobInstance) {
    Instant roundedDownTime = DateTimeUtils.roundDownTo5MinBoundary(getActivityStartTime());
    Instant preactivityStart = roundedDownTime.minus(verificationJobInstance.getResolvedJob().getDuration());

    verificationJobInstance.setPreActivityVerificationStartTime(preactivityStart);
    verificationJobInstance.setPostActivityVerificationStartTime(roundedDownTime);
    verificationJobInstance.setStartTime(preactivityStart);
  }

  @Override
  public void validateActivityParams() {
    //
  }
}
