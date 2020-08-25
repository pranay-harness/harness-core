package io.harness.cvng.core.beans;

import io.harness.cvng.core.entities.Activity;
import io.harness.cvng.core.entities.Activity.ActivityType;
import io.harness.cvng.core.entities.DeploymentActivity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.HashSet;
import java.util.Set;

@Value
@Builder
@AllArgsConstructor
public class DeploymentActivityDTO extends ActivityDTO {
  Long dataCollectionDelayMs;
  Set<String> oldVersionHosts;
  Set<String> newVersionHosts;
  Integer newHostsTrafficSplitPercentage;

  @Override
  public ActivityType getType() {
    return ActivityType.DEPLOYMENT;
  }

  @Override
  public Activity toEntity() {
    DeploymentActivity deploymentActivity = DeploymentActivity.builder()
                                                .dataCollectionDelayMs(dataCollectionDelayMs)
                                                .oldVersionHosts(new HashSet<>(oldVersionHosts))
                                                .newVersionHosts(new HashSet<>(newVersionHosts))
                                                .newHostsTrafficSplitPercentage(newHostsTrafficSplitPercentage)
                                                .build();
    super.addCommonDataFields(deploymentActivity);
    return deploymentActivity;
  }
}
