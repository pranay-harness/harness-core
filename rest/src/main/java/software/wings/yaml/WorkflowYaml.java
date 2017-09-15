package software.wings.yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Workflow;

public class WorkflowYaml extends GenericYaml {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @YamlSerialize public String name;
  @YamlSerialize public String description;
  @YamlSerialize public String workflowType;

  public WorkflowYaml() {}

  public WorkflowYaml(Workflow workflow) {
    this.name = workflow.getName();
    this.description = workflow.getDescription();
    this.workflowType = workflow.getWorkflowType().toString();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getWorkflowType() {
    return workflowType;
  }

  public void setWorkflowType(String workflowType) {
    this.workflowType = workflowType;
  }
}