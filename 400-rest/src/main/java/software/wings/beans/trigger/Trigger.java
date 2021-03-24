package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.WorkflowType.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.trigger.TriggerConditionType.WEBHOOK;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.WorkflowType;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.NameAccess;

import software.wings.beans.Base;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.entityinterface.ApplicationAccess;
import software.wings.beans.entityinterface.TagAware;
import software.wings.beans.trigger.ArtifactSelection.ArtifactSelectionKeys;
import software.wings.beans.trigger.ArtifactTriggerCondition.ArtifactTriggerConditionKeys;
import software.wings.beans.trigger.ManifestTriggerCondition.ManifestTriggerConditionKeys;
import software.wings.beans.trigger.TriggerCondition.TriggerConditionKeys;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by sgurubelli on 10/25/17.
 */
@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "TriggerKeys")
@Entity(value = "triggers")
@HarnessEntity(exportable = true)

public class Trigger extends Base implements NameAccess, TagAware, ApplicationAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("yaml")
                 .unique(true)
                 .field(TriggerKeys.appId)
                 .field(TriggerKeys.name)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("conditionArtifactStreamId")
                 .field(TriggerKeys.condition + "." + TriggerConditionKeys.conditionType)
                 .field(TriggerKeys.condition + "." + ArtifactTriggerConditionKeys.artifactStreamId)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("conditionAppManifestId")
                 .field(TriggerKeys.condition + "." + TriggerConditionKeys.conditionType)
                 .field(TriggerKeys.condition + "." + ManifestTriggerConditionKeys.appManifestId)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("artifactSelectionsArtifactStreamId")
                 .field(TriggerKeys.artifactSelections + "." + ArtifactSelectionKeys.artifactStreamId)
                 .build())
        .build();
  }

  @NotEmpty private String name;
  @FdIndex private String accountId;
  private String description;
  @NotNull private TriggerCondition condition;
  @Deprecated private String pipelineId;
  private String pipelineName;
  private String workflowId;
  private String workflowName;
  private List<ArtifactSelection> artifactSelections = new ArrayList<>();
  private List<ManifestSelection> manifestSelections = new ArrayList<>();
  @JsonIgnore @FdIndex private String webHookToken;
  private WorkflowType workflowType;
  private Map<String, String> workflowVariables;
  // If any variable is Runtime and Default values is provided
  private boolean continueWithDefaultValues;
  private List<ServiceInfraWorkflow> serviceInfraWorkflows;
  private boolean excludeHostsWithSameArtifact;
  private transient List<HarnessTagLink> tagLinks;
  private boolean disabled;

  @Builder
  public Trigger(String uuid, String appId, String accountId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String name, String description,
      TriggerCondition condition, String pipelineId, String pipelineName, String workflowId, String workflowName,
      List<ArtifactSelection> artifactSelections, List<ManifestSelection> manifestSelections, String webHookToken,
      WorkflowType workflowType, Map<String, String> workflowVariables,
      List<ServiceInfraWorkflow> serviceInfraWorkflows, boolean excludeHostsWithSameArtifact,
      boolean continueWithDefaultValues) {
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
    this.excludeHostsWithSameArtifact = excludeHostsWithSameArtifact;
    this.manifestSelections = manifestSelections;
    this.accountId = accountId;
    this.continueWithDefaultValues = continueWithDefaultValues;
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

  @UtilityClass
  public static final class TriggerKeys {
    // Temporary
    public static final String appId = "appId";
    public static final String createdAt = "createdAt";
    public static final String uuid = "uuid";
  }
}
