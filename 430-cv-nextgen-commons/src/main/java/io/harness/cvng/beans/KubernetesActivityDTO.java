package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeName("INFRASTRUCTURE")
@EqualsAndHashCode(callSuper = true)
public class KubernetesActivityDTO extends ActivityDTO {
  String clusterName;
  String activityDescription;
  String activitySourceConfigId;

  @Override
  public ActivityType getType() {
    return ActivityType.INFRASTRUCTURE;
  }
}
