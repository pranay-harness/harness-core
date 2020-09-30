package io.harness.cvng.core.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
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
@JsonTypeName("DEPLOYMENT")
@AllArgsConstructor
public class DeploymentActivityDTO extends ActivityDTO {
  Long dataCollectionDelayMs;
  Set<String> oldVersionHosts;
  Set<String> newVersionHosts;
  Integer newHostsTrafficSplitPercentage;
  String deploymentTag;
  Long verificationStartTime;

  @Override
  public ActivityType getType() {
    return ActivityType.DEPLOYMENT;
  }

  @Override
  public Activity toEntity() {
    DeploymentActivity deploymentActivity =
        DeploymentActivity.builder()
            .dataCollectionDelayMs(dataCollectionDelayMs)
            .oldVersionHosts(oldVersionHosts == null ? null : new HashSet<>(oldVersionHosts))
            .newVersionHosts(newVersionHosts == null ? null : new HashSet<>(newVersionHosts))
            .newHostsTrafficSplitPercentage(newHostsTrafficSplitPercentage)
            .deploymentTag(deploymentTag)
            .verificationStartTime(verificationStartTime)
            .build();
    deploymentActivity.setType(ActivityType.DEPLOYMENT);
    super.addCommonDataFields(deploymentActivity);
    return deploymentActivity;
  }
}
