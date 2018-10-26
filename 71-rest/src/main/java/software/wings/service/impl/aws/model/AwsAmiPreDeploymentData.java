package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AwsAmiPreDeploymentData {
  private Map<String, Integer> asgNameToMinCapacity;
  private Map<String, Integer> asgNameToDesiredCapacity;
  private static final int DEFAULT_DESIRED_COUNT = 10;

  public int getPreDeploymentMinCapacity(String asgName) {
    if (asgNameToMinCapacity == null) {
      return 0;
    }
    Integer minCapacity = asgNameToMinCapacity.get(asgName);
    return minCapacity == null ? 0 : minCapacity;
  }

  public int getPreDeploymentDesiredCapacity(String asgName) {
    if (asgNameToDesiredCapacity == null) {
      return DEFAULT_DESIRED_COUNT;
    }
    Integer minCapacity = asgNameToDesiredCapacity.get(asgName);
    return minCapacity == null ? DEFAULT_DESIRED_COUNT : minCapacity;
  }

  public boolean hasAsgReachedPreDeploymentCount(String asgName, int desiredCount) {
    if (asgNameToDesiredCapacity == null) {
      return false;
    }
    Integer count = asgNameToDesiredCapacity.get(asgName);
    if (count == null) {
      return false;
    }
    return count.equals(desiredCount);
  }
}