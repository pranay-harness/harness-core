package io.harness.cvng.core.entities;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
@JsonTypeName("INFRASTRUCTURE")
@Data
@Builder
@AllArgsConstructor
public class KubernetesActivity extends Activity {
  private String clusterName;
  private String activityDescription;

  @Override
  public ActivityType getType() {
    return ActivityType.INFRASTRUCTURE;
  }
}
