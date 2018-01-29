package software.wings.beans.trigger;

import static software.wings.beans.WorkflowType.PIPELINE;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.WorkflowType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by sgurubelli on 10/25/17.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity(value = "triggers")
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("name") }, options = @IndexOptions(unique = true)))
public class Trigger extends Base {
  @NotEmpty private String name;
  private String description;
  @NotNull private TriggerCondition condition;
  private String pipelineId;
  private String pipelineName;
  private String workflowId;
  private String workflowName;
  private List<ArtifactSelection> artifactSelections = new ArrayList<>();
  @JsonIgnore @Indexed private String webHookToken;
  private WorkflowType workflowType;
  private Map<String, String> workflowVariables;
  private List<ServiceInfraWorkflow> serviceInfraWorkflows;

  public void setPipelineId(String pipelineId) {
    this.pipelineId = pipelineId;
    this.workflowId = pipelineId;
  }

  public void setPipelineName(String pipelineName) {
    this.pipelineName = pipelineName;
    this.workflowName = pipelineName;
  }

  public String getPipelineId() {
    if (this.pipelineId == null) {
      return this.workflowId;
    }
    return pipelineId;
  }

  public String getPipelineName() {
    if (this.pipelineName == null) {
      return this.workflowName;
    }
    return this.pipelineName;
  }

  public String getWorkflowId() {
    if (workflowId == null) {
      return pipelineId;
    }
    return workflowId;
  }

  public String getWorkflowName() {
    if (workflowName == null) {
      return pipelineName;
    }
    return workflowName;
  }

  public static final class Builder {
    protected String appId;
    private String uuid;
    private String name;
    private String description;
    private TriggerCondition condition;
    private String pipelineId;
    private String workflowId;
    private List<ArtifactSelection> artifactSelections = new ArrayList<>();
    private WorkflowType workflowType = PIPELINE;
    private Map<String, String> workflowVariables;
    List<ServiceInfraWorkflow> serviceInfraWorkflows;

    private Builder() {}

    public static Builder aTrigger() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withCondition(TriggerCondition condition) {
      this.condition = condition;
      return this;
    }

    public Builder withPipelineId(String pipelineId) {
      this.pipelineId = pipelineId;
      this.workflowId = pipelineId;
      return this;
    }

    public Builder withWorkflowId(String workflowId) {
      this.workflowId = workflowId;
      return this;
    }

    public Builder withArtifactSelections(List<ArtifactSelection> artifactSelections) {
      this.artifactSelections = artifactSelections;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withWorkflowType(WorkflowType workflowType) {
      this.workflowType = workflowType;
      return this;
    }

    public Builder withWorkflowVariables(Map<String, String> workflowVariables) {
      this.workflowVariables = workflowVariables;
      return this;
    }

    public Builder withServiceInfraWorkflows(List<ServiceInfraWorkflow> serviceInfraWorkflows) {
      this.serviceInfraWorkflows = serviceInfraWorkflows;
      return this;
    }

    public Trigger build() {
      Trigger trigger = new Trigger();
      trigger.setName(name);
      trigger.setDescription(description);
      trigger.setCondition(condition);
      trigger.setPipelineId(pipelineId);
      trigger.setArtifactSelections(artifactSelections);
      trigger.setAppId(appId);
      trigger.setUuid(uuid);
      trigger.setWorkflowId(workflowId);
      trigger.setWorkflowType(workflowType);
      trigger.setWorkflowVariables(workflowVariables);
      trigger.setServiceInfraWorkflows(serviceInfraWorkflows);
      return trigger;
    }
  }
}
