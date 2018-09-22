package software.wings.yaml.artifactstream;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.yaml.BaseYaml;

@Data
@EqualsAndHashCode(callSuper = false)
public class StreamActionYaml extends BaseYaml {
  public String workflowType;
  public String workflowName;
  public String envName;

  public static final class Builder {
    public String workflowType;
    public String workflowName;
    public String envName;

    private Builder() {}

    public static Builder aStreamActionYaml() {
      return new Builder();
    }

    public Builder withWorkflowType(String workflowType) {
      this.workflowType = workflowType;
      return this;
    }

    public Builder withWorkflowName(String workflowName) {
      this.workflowName = workflowName;
      return this;
    }

    public Builder withEnvName(String envName) {
      this.envName = envName;
      return this;
    }

    public Builder but() {
      return aStreamActionYaml().withWorkflowType(workflowType).withWorkflowName(workflowName).withEnvName(envName);
    }

    public StreamActionYaml build() {
      StreamActionYaml streamActionYaml = new StreamActionYaml();
      streamActionYaml.setWorkflowType(workflowType);
      streamActionYaml.setWorkflowName(workflowName);
      streamActionYaml.setEnvName(envName);
      return streamActionYaml;
    }
  }
}
