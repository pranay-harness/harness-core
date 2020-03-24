package software.wings.beans.trigger;

import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.trigger.TriggerConditionType.WEBHOOK;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.WorkflowType;
import io.harness.persistence.NameAccess;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.AllowedValueYaml;
import software.wings.beans.Base;
import software.wings.beans.EntityType;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.NameValuePair;
import software.wings.beans.entityinterface.ApplicationAccess;
import software.wings.beans.entityinterface.TagAware;
import software.wings.beans.trigger.ArtifactSelection.ArtifactSelectionKeys;
import software.wings.beans.trigger.ArtifactTriggerCondition.ArtifactTriggerConditionKeys;
import software.wings.beans.trigger.Trigger.TriggerKeys;
import software.wings.beans.trigger.TriggerCondition.TriggerConditionKeys;
import software.wings.yaml.BaseEntityYaml;
import software.wings.yaml.trigger.TriggerConditionYaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by sgurubelli on 10/25/17.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "TriggerKeys")
@Entity(value = "triggers")
@HarnessEntity(exportable = true)
@Indexes({
  @Index(options = @IndexOptions(name = "yaml", unique = true), fields = { @Field("appId")
                                                                           , @Field("name") })
  , @Index(options = @IndexOptions(name = "conditionArtifactStreamId"), fields = {
    @Field(TriggerKeys.condition + "." + TriggerConditionKeys.conditionType)
    , @Field(TriggerKeys.condition + "." + ArtifactTriggerConditionKeys.artifactStreamId)
  }), @Index(options = @IndexOptions(name = "artifactSelectionsArtifactStreamId"), fields = {
    @Field(TriggerKeys.artifactSelections + "." + ArtifactSelectionKeys.artifactStreamId)
  })
})
public class Trigger extends Base implements NameAccess, TagAware, ApplicationAccess {
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
  private boolean excludeHostsWithSameArtifact;
  private transient List<HarnessTagLink> tagLinks;
  private boolean disabled;

  @Builder
  public Trigger(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, String entityYamlPath, String name, String description, TriggerCondition condition,
      String pipelineId, String pipelineName, String workflowId, String workflowName,
      List<ArtifactSelection> artifactSelections, String webHookToken, WorkflowType workflowType,
      Map<String, String> workflowVariables, List<ServiceInfraWorkflow> serviceInfraWorkflows) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.name = name;
    this.description = description;
    this.condition = condition;
    this.pipelineId = pipelineId;
    this.pipelineName = pipelineName;
    this.workflowId = workflowId;
    this.workflowName = workflowName;
    this.artifactSelections = (artifactSelections == null) ? new ArrayList<>() : artifactSelections;
    this.webHookToken = webHookToken;
    this.workflowType = workflowType != null ? workflowType : PIPELINE;
    this.workflowVariables = workflowVariables;
    this.serviceInfraWorkflows = serviceInfraWorkflows;
  }

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

  public String getWorkflowId() {
    if (workflowId == null) {
      return pipelineId;
    }
    return workflowId;
  }

  public String fetchWorkflowOrPipelineName() {
    if (workflowName == null) {
      return pipelineName;
    }
    return workflowName;
  }

  public Map<String, String> getWorkflowVariables() {
    // TODO: This is temporary code till we migrate all the triggers
    if (condition != null && WEBHOOK == condition.getConditionType()) {
      WebHookTriggerCondition webHookTriggerCondition = (WebHookTriggerCondition) condition;
      if (isNotEmpty(webHookTriggerCondition.getParameters())) {
        if (workflowVariables == null) {
          workflowVariables = new LinkedHashMap<>();
        }
        workflowVariables.putAll(webHookTriggerCondition.getParameters());
      }
    }
    return workflowVariables;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseEntityYaml {
    private String description;
    @NotEmpty List<TriggerConditionYaml> triggerCondition = new ArrayList<>();
    private String executionType;
    private String executionName;
    private List<ArtifactSelection.Yaml> artifactSelections = new ArrayList<>();
    private List<TriggerVariable> workflowVariables = new ArrayList<>();

    @lombok.Builder
    public Yaml(String harnessApiVersion, String description, String executionType, String executionName,
        List<TriggerVariable> workflowVariables, List<TriggerConditionYaml> triggerCondition,
        List<ArtifactSelection.Yaml> artifactSelections) {
      super(EntityType.TRIGGER.name(), harnessApiVersion);
      this.setHarnessApiVersion(harnessApiVersion);
      this.description = description;
      this.executionType = executionType;
      this.executionName = executionName;
      this.workflowVariables = workflowVariables;
      this.triggerCondition = triggerCondition;
      this.artifactSelections = artifactSelections;
    }

    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class TriggerVariable extends NameValuePair.AbstractYaml {
      String entityType;

      @Builder
      public TriggerVariable(
          String entityType, String name, String value, String valueType, List<AllowedValueYaml> allowedValueYamls) {
        super(name, value, valueType, allowedValueYamls);
        this.entityType = entityType;
      }
    }
  }

  @UtilityClass
  public static final class TriggerKeys {
    // Temporary
    public static final String appId = "appId";
    public static final String createdAt = "createdAt";
    public static final String uuid = "uuid";
  }
}
