package io.harness.cvng.activity.entities;

import io.harness.cvng.beans.ActivityType;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;

import java.time.Instant;

public abstract class InfrastructureActivity extends Activity {
  @Override
  public ActivityType getType() {
    return ActivityType.INFRASTRUCTURE;
  }

  @Override
  public void fillInVerificationJobInstanceDetails(VerificationJobInstance verificationJobInstance) {
    Instant roundedDownTime = DateTimeUtils.roundDownTo5MinBoundary(getActivityStartTime());
    Instant preactivityStart = roundedDownTime.minus(verificationJobInstance.getResolvedJob().getDuration());

    verificationJobInstance.setPreActivityVerificationStartTime(preactivityStart);
    verificationJobInstance.setPostActivityVerificationStartTime(roundedDownTime);
    verificationJobInstance.setStartTime(preactivityStart);
  }
}
