package software.wings.beans;

import com.google.common.base.MoreObjects;

import software.wings.sm.ExecutionStatus;

import java.util.List;
import java.util.Objects;

/**
 * Created by peeyushaggarwal on 7/7/16.
 */
public class SimpleWorkflowDetails {
  private ExecutionStrategy executionStrategy;
  private List<Instance> instances;

  public ExecutionStrategy getExecutionStrategy() {
    return executionStrategy;
  }

  public void setExecutionStrategy(ExecutionStrategy executionStrategy) {
    this.executionStrategy = executionStrategy;
  }

  public List<Instance> getInstances() {
    return instances;
  }

  public void setInstances(List<Instance> instances) {
    this.instances = instances;
  }

  public static final class Builder {
    private ExecutionStrategy executionStrategy;
    private List<Instance> instances;

    private Builder() {}

    public static Builder aSimpleWorkflowDetails() {
      return new Builder();
    }

    public Builder withExecutionStrategy(ExecutionStrategy executionStrategy) {
      this.executionStrategy = executionStrategy;
      return this;
    }

    public Builder withInstances(List<Instance> instances) {
      this.instances = instances;
      return this;
    }

    public Builder but() {
      return aSimpleWorkflowDetails().withExecutionStrategy(executionStrategy).withInstances(instances);
    }

    public SimpleWorkflowDetails build() {
      SimpleWorkflowDetails simpleWorkflowDetails = new SimpleWorkflowDetails();
      simpleWorkflowDetails.setExecutionStrategy(executionStrategy);
      simpleWorkflowDetails.setInstances(instances);
      return simpleWorkflowDetails;
    }
  }

  /**
   * Created by peeyushaggarwal on 7/7/16.
   */
  public static class Instance {
    private String hostName;
    private String templateName;
    private ExecutionStatus status = ExecutionStatus.SCHEDULED;

    /**
     * Getter for property 'hostName'.
     *
     * @return Value for property 'hostName'.
     */
    public String getHostName() {
      return hostName;
    }

    /**
     * Setter for property 'hostName'.
     *
     * @param hostName Value to set for property 'hostName'.
     */
    public void setHostName(String hostName) {
      this.hostName = hostName;
    }

    /**
     * Getter for property 'templateName'.
     *
     * @return Value for property 'templateName'.
     */
    public String getTemplateName() {
      return templateName;
    }

    /**
     * Setter for property 'templateName'.
     *
     * @param templateName Value to set for property 'templateName'.
     */
    public void setTemplateName(String templateName) {
      this.templateName = templateName;
    }

    /**
     * Getter for property 'status'.
     *
     * @return Value for property 'status'.
     */
    public ExecutionStatus getStatus() {
      return status;
    }

    /**
     * Setter for property 'status'.
     *
     * @param status Value to set for property 'status'.
     */
    public void setStatus(ExecutionStatus status) {
      this.status = status;
    }

    @Override
    public int hashCode() {
      return Objects.hash(hostName, templateName);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("hostName", hostName)
          .add("templateName", templateName)
          .add("status", status)
          .toString();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final Instance other = (Instance) obj;
      return Objects.equals(this.hostName, other.hostName) && Objects.equals(this.templateName, other.templateName);
    }

    public static final class Builder {
      private String hostName;
      private String templateName;
      private ExecutionStatus status = ExecutionStatus.SCHEDULED;

      private Builder() {}

      public static Builder anInstance() {
        return new Builder();
      }

      public Builder withHostName(String hostName) {
        this.hostName = hostName;
        return this;
      }

      public Builder withTemplateName(String templateName) {
        this.templateName = templateName;
        return this;
      }

      public Builder withStatus(ExecutionStatus status) {
        this.status = status;
        return this;
      }

      public Builder but() {
        return anInstance().withHostName(hostName).withTemplateName(templateName).withStatus(status);
      }

      public Instance build() {
        Instance instance = new Instance();
        instance.setHostName(hostName);
        instance.setTemplateName(templateName);
        instance.setStatus(status);
        return instance;
      }
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("executionStrategy", executionStrategy)
        .add("instances", instances)
        .toString();
  }
}
