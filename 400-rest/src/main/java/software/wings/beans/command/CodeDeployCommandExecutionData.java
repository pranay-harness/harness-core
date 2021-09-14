/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.command;

import io.harness.shell.CommandExecutionData;

import com.amazonaws.services.ec2.model.Instance;
import java.util.ArrayList;
import java.util.List;

public class CodeDeployCommandExecutionData implements CommandExecutionData {
  /**
   * The Instances.
   */
  List<Instance> instances = new ArrayList<>();
  private String deploymentId;

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

  /**
   * The type Builder.
   */
  public static final class Builder {
    /**
     * The Instances.
     */
    List<Instance> instances = new ArrayList<>();
    private String deploymentId;

    private Builder() {}

    /**
     * A code deploy command execution data builder.
     *
     * @return the builder
     */
    public static Builder aCodeDeployCommandExecutionData() {
      return new Builder();
    }

    /**
     * With instances builder.
     *
     * @param instances the instances
     * @return the builder
     */
    public Builder withInstances(List<Instance> instances) {
      this.instances = instances;
      return this;
    }

    /**
     * With deployment id builder.
     *
     * @param deploymentId the deployment id
     * @return the builder
     */
    public Builder withDeploymentId(String deploymentId) {
      this.deploymentId = deploymentId;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aCodeDeployCommandExecutionData().withInstances(instances).withDeploymentId(deploymentId);
    }

    /**
     * Build code deploy command execution data.
     *
     * @return the code deploy command execution data
     */
    public CodeDeployCommandExecutionData build() {
      CodeDeployCommandExecutionData codeDeployCommandExecutionData = new CodeDeployCommandExecutionData();
      codeDeployCommandExecutionData.setInstances(instances);
      codeDeployCommandExecutionData.setDeploymentId(deploymentId);
      return codeDeployCommandExecutionData;
    }
  }
}
