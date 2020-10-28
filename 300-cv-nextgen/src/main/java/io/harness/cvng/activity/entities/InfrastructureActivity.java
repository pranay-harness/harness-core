package io.harness.cvng.activity.entities;

import io.harness.cvng.beans.ActivityType;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;

import java.time.Instant;

public abstract class InfrastructureActivity extends Activity {
  @Override
  public ActivityType getType() {
    return ActivityType.INFRASTRUCTURE;
  }

  @Override
  public void fillInVerificationJobInstanceDetails(VerificationJobInstance verificationJobInstance) {
    Instant preactivityStart =
        this.getActivityStartTime().minus(verificationJobInstance.getResolvedJob().getDuration());
    verificationJobInstance.setPreActivityVerificationStartTime(preactivityStart);
    verificationJobInstance.setPostActivityVerificationStartTime(this.getActivityStartTime());
    verificationJobInstance.setStartTime(preactivityStart);
  }
}
