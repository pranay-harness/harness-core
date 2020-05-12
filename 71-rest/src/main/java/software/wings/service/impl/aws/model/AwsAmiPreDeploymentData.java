package software.wings.service.impl.aws.model;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AwsAmiPreDeploymentData {
  private String oldAsgName;
  private Integer minCapacity;
  private Integer desiredCapacity;
  private List<String> scalingPolicyJSON;

  public static final int DEFAULT_DESIRED_COUNT = 10;

  public int getPreDeploymentMinCapacity() {
    return minCapacity == null ? 0 : minCapacity;
  }

  public int getPreDeploymentDesiredCapacity() {
    return desiredCapacity == null ? DEFAULT_DESIRED_COUNT : desiredCapacity;
  }

  public boolean hasAsgReachedPreDeploymentCount(int desiredCount) {
    if (desiredCapacity == null) {
      return false;
    }

    return desiredCapacity.intValue() == desiredCount;
  }

  public List<String> getPreDeploymenyScalingPolicyJSON() {
    return isNotEmpty(scalingPolicyJSON) ? scalingPolicyJSON : emptyList();
  }
}