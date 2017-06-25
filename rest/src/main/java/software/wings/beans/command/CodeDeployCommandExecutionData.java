package software.wings.beans.command;

import com.amazonaws.services.ec2.model.Instance;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 6/23/17.
 */
public class CodeDeployCommandExecutionData extends CommandExecutionData {
  /**
   * The Instances.
   */
  List<Instance> instances = new ArrayList<>();

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

  /**
   * The type Builder.
   */
  public static final class Builder {
    /**
     * The Instances.
     */
    List<Instance> instances = new ArrayList<>();

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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aCodeDeployCommandExecutionData().withInstances(instances);
    }

    /**
     * Build code deploy command execution data.
     *
     * @return the code deploy command execution data
     */
    public CodeDeployCommandExecutionData build() {
      CodeDeployCommandExecutionData codeDeployCommandExecutionData = new CodeDeployCommandExecutionData();
      codeDeployCommandExecutionData.setInstances(instances);
      return codeDeployCommandExecutionData;
    }
  }
}
