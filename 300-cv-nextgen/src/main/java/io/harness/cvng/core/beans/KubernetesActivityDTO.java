package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cvng.core.entities.Activity;
import io.harness.cvng.core.entities.Activity.ActivityType;
import io.harness.cvng.core.entities.KubernetesActivity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@JsonTypeName("INFRASTRUCTURE")
@Builder
@AllArgsConstructor
public class KubernetesActivityDTO extends ActivityDTO {
  String clusterName;
  String activityDescription;

  @Override
  public ActivityType getType() {
    return ActivityType.INFRASTRUCTURE;
  }

  public Activity toEntity() {
    KubernetesActivity kubernetesActivity =
        KubernetesActivity.builder().clusterName(clusterName).activityDescription(activityDescription).build();
    super.addCommonDataFields(kubernetesActivity);
    kubernetesActivity.setType(ActivityType.INFRASTRUCTURE);
    return kubernetesActivity;
  }
}
