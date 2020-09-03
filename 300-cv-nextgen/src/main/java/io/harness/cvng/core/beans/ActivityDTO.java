package io.harness.cvng.core.beans;

import io.harness.cvng.core.entities.Activity;
import io.harness.cvng.core.entities.Activity.ActivityType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public abstract class ActivityDTO {
  @NotNull private String accountIdentifier;
  @NotNull private String projectIdentifier;
  @NotNull private String orgIdentifier;
  private String serviceIdentifier;
  @NotNull private String environmentIdentifier;
  @NotNull private String name;
  private List<String> verificationJobsToTrigger;
  @NotNull private Long activityStartTime;
  private Long activityEndTime;

  public abstract ActivityType getType();
  public abstract Activity toEntity();

  public void addCommonDataFields(Activity activity) {
    activity.setAccountIdentifier(accountIdentifier);
    activity.setProjectIdentifier(projectIdentifier);
    activity.setOrgIdentifier(orgIdentifier);
    activity.setServiceIdentifier(serviceIdentifier);
    activity.setEnvironmentIdentifier(environmentIdentifier);
    activity.setActivityName(name);
    activity.setVerificationJobsToTrigger(
        verificationJobsToTrigger == null ? null : new ArrayList<>(verificationJobsToTrigger));
    activity.setActivityStartTime(Instant.ofEpochMilli(activityStartTime));
    activity.setActivityEndTime(activityEndTime != null ? Instant.ofEpochMilli(activityEndTime) : null);
  }
}
