package software.wings.cloudprovider;

import com.amazonaws.services.ec2.model.Instance;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

import java.util.List;

/**
 * Created by anubhaw on 6/23/17.
 */
public class CodeDeployDeploymentInfo {
  private CommandExecutionStatus status;
  private List<Instance> instances;
  private String deploymentId;

  /**
   * Gets status.
   *
   * @return the status
   */
  public CommandExecutionStatus getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(CommandExecutionStatus status) {
    this.status = status;
  }

  /**
   * Gets instances.
   *
   * @return the instances
   */
  public List<Instance> getInstances() {
    return instances;
  }

  /**
   * Sets instances.
   *
   * @param instances the instances
   */
  public void setInstances(List<Instance> instances) {
    this.instances = instances;
  }

  public String getDeploymentId() {
    return deploymentId;
  }

  public void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  @Override
  public String toString() {
    return new StringBuffer("CodeDeployDeploymentInfo{")
        .append("status=")
        .append(status)
        .append(", instances=")
        .append(instances)
        .append(", deploymentId='")
        .append(deploymentId)
        .append('\'')
        .append('}')
        .toString();
  }
}
