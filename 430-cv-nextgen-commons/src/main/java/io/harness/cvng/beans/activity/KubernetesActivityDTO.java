package io.harness.cvng.beans.activity;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KUBERNETES")
public class KubernetesActivityDTO extends ActivityDTO {
  String message;
  String activitySourceConfigId;
  String eventDetails;
  KubernetesEventType eventType;
  ActivityType kubernetesActivityType;

  @Override
  public ActivityType getType() {
    return ActivityType.KUBERNETES;
  }

  public enum KubernetesEventType { Normal, Warning, Error }
}
