package software.wings.beans.trigger;

import io.harness.annotation.HarnessExportableEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.WorkflowType;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;

import javax.validation.constraints.NotNull;

/**
 * Created by sgurubelli on 10/25/17.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity(value = "deploymentTriggers")
@Indexes(@Index(
    options = @IndexOptions(name = "uniqueTriggerIdx", unique = true), fields = { @Field("appId")
                                                                                  , @Field("name") }))
@HarnessExportableEntity
public class DeploymentTrigger extends Base {
  @EntityName @NotEmpty @Trimmed private String name;
  private String description;
  @NotEmpty private String workflowId;
  private String workflowName;
  @NotNull private WorkflowType workflowType;
  @NotNull private Condition condition;

  @Builder
  public DeploymentTrigger(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String name, String description,
      Condition condition, String workflowId, String workflowName, WorkflowType workflowType) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.name = name;
    this.description = description;
    this.condition = condition;
    this.workflowId = workflowId;
    this.workflowName = workflowName;
    this.workflowType = workflowType;
  }
}
