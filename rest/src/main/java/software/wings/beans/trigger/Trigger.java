package software.wings.beans.trigger;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.WorkflowType;

import java.util.List;

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
  private TriggerCondition condition;
  @NotEmpty private String pipelineId;

  private List<ArtifactSelection> artifactSelections;

  public static final class Builder {
    protected String appId;
    private String uuid;
    private String name;
    private String description;
    private TriggerCondition condition;
    private String pipelineId;
    private List<ArtifactSelection> advanceConfigurations;

    private Builder() {}

    public static Builder aDeploymentTrigger() {
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
      return this;
    }

    public Builder withAdvanceConfigurations(List<ArtifactSelection> advanceConfigurations) {
      this.advanceConfigurations = advanceConfigurations;
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
      return this;
    }

    public Builder but() {
      return aDeploymentTrigger()
          .withName(name)
          .withDescription(description)
          .withCondition(condition)
          .withPipelineId(pipelineId)
          .withAdvanceConfigurations(advanceConfigurations)
          .withAppId(appId)
          .withUuid(uuid);
    }

    public Trigger build() {
      Trigger trigger = new Trigger();
      trigger.setName(name);
      trigger.setDescription(description);
      trigger.setCondition(condition);
      trigger.setPipelineId(pipelineId);
      trigger.setArtifactSelections(advanceConfigurations);
      trigger.setAppId(appId);
      trigger.setUuid(uuid);
      return trigger;
    }
  }
}
