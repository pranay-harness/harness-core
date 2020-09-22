package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.cvng.core.entities.Activity;
import io.harness.cvng.core.entities.Activity.ActivityType;
import io.harness.cvng.core.entities.Activity.VerificationJobRuntimeDetails;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class ActivityDTO {
  @NotNull private String accountIdentifier;
  @NotNull private String projectIdentifier;
  @NotNull private String orgIdentifier;
  private String serviceIdentifier;
  @NotNull private String environmentIdentifier;
  @NotNull private String name;
  private List<VerificationJobRuntimeDetails> verificationJobRuntimeDetails;
  @NotNull private Long activityStartTime;
  private Long activityEndTime;
  private List<String> tags;

  public abstract ActivityType getType();
  public abstract Activity toEntity();

  public void addCommonDataFields(Activity activity) {
    activity.setAccountIdentifier(accountIdentifier);
    activity.setProjectIdentifier(projectIdentifier);
    activity.setOrgIdentifier(orgIdentifier);
    activity.setServiceIdentifier(serviceIdentifier);
    activity.setEnvironmentIdentifier(environmentIdentifier);
    activity.setActivityName(name);
    activity.setVerificationJobRuntimeDetails(
        verificationJobRuntimeDetails == null ? null : verificationJobRuntimeDetails);
    activity.setActivityStartTime(Instant.ofEpochMilli(activityStartTime));
    activity.setActivityEndTime(activityEndTime != null ? Instant.ofEpochMilli(activityEndTime) : null);
    activity.setTags(tags);
  }
}
