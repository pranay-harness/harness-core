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
  String namespace;
  String workloadName;
  String reason;
  String message;
  String kind;
  String activitySourceConfigId;
  String eventJson;
  @Deprecated KubernetesEventType eventType;

  @Override
  public ActivityType getType() {
    return ActivityType.KUBERNETES;
  }

  public enum KubernetesEventType { Normal, Warning, Error }
}
