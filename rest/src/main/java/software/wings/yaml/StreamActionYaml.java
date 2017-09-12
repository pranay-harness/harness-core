package software.wings.yaml;

public class StreamActionYaml extends GenericYaml {
  @YamlSerialize public String workflowType;
  @YamlSerialize public String workflowName;

  public String getWorkflowType() {
    return workflowType;
  }

  public void setWorkflowType(String workflowType) {
    this.workflowType = workflowType;
  }

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }
}
