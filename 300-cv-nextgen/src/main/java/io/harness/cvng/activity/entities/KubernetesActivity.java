package io.harness.cvng.activity.entities;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceBuilder;
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
  String namespace;
  String workloadName;
  String kind;
  Set<KubernetesActivityDTO> activities;
  @FdIndex Instant bucketStartTime;

  @Override
  public ActivityType getType() {
    return ActivityType.KUBERNETES;
  }

  @Override
  public void fromDTO(ActivityDTO activityDTO) {
    throw new NotImplementedException();
  }

  @Override
  public void fillInVerificationJobInstanceDetails(VerificationJobInstanceBuilder verificationJobInstanceBuilder) {
    Instant roundedDownTime = DateTimeUtils.roundDownTo5MinBoundary(getActivityStartTime());
    Instant preactivityStart = roundedDownTime.minus(verificationJobInstanceBuilder.getResolvedJob().getDuration());

    verificationJobInstanceBuilder.preActivityVerificationStartTime(preactivityStart);
    verificationJobInstanceBuilder.postActivityVerificationStartTime(roundedDownTime);
    verificationJobInstanceBuilder.startTime(preactivityStart);
  }

  @Override
  public void validateActivityParams() {}

  @Override
  public String getActivityName() {
    return activities.size() + " kubernetes events";
  }

  @Override
  public boolean deduplicateEvents() {
    return true;
  }
}
