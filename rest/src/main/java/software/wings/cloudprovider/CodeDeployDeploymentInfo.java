package software.wings.cloudprovider;

import com.google.common.base.MoreObjects;

import com.amazonaws.services.ec2.model.Instance;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

import java.util.List;

/**
 * Created by anubhaw on 6/23/17.
 */
public class CodeDeployDeploymentInfo {
  private CommandExecutionStatus status;
  private List<Instance> instances;

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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("status", status).add("instances", instances).toString();
  }
}
