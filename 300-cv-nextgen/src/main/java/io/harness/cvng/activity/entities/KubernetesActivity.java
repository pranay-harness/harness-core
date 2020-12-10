package io.harness.cvng.activity.entities;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.cvng.beans.activity.KubernetesActivityDTO.KubernetesEventType;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.mongo.index.FdIndex;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@Data
@FieldNameConstants(innerTypeName = "KubernetesActivityKeys")
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KUBERNETES")
public class KubernetesActivity extends Activity {
  KubernetesEventType eventType;
  Set<KubernetesActivityDTO> activities;
  @FdIndex Instant bucketStartTime;
  ActivityType kubernetesActivityType;

  @Override
  public ActivityType getType() {
    return ActivityType.KUBERNETES;
  }

  @Override
  public void fromDTO(ActivityDTO activityDTO) {
    throw new NotImplementedException();
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
  public void validateActivityParams() {}

  @Override
  public String getActivityName() {
    return activities.size() + " " + eventType.name() + " kubernetes events";
  }
}
