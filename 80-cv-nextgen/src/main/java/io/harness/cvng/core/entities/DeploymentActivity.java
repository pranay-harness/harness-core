package io.harness.cvng.core.entities;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@JsonTypeName("DEPLOYMENT")
@Data
@Builder
@AllArgsConstructor
public class DeploymentActivity extends Activity {
  Long dataCollectionDelayMs;
  Set<String> oldVersionHosts;
  Set<String> newVersionHosts;
  Integer newHostsTrafficSplitPercentage;

  @Override
  public ActivityType getType() {
    return ActivityType.DEPLOYMENT;
  }
}
