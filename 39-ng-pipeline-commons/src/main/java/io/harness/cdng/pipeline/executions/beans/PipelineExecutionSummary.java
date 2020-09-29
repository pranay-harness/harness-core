package io.harness.cdng.pipeline.executions.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.EmbeddedUser;
import io.harness.cdng.pipeline.executions.ExecutionStatus;
import io.harness.cdng.pipeline.executions.TriggerType;
import io.harness.cdng.pipeline.executions.beans.PipelineExecutionSummary.PipelineExecutionSummaryKeys;
import io.harness.mongo.index.CdUniqueIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.PersistentEntity;
import io.harness.pipeline.executions.NGStageType;
import io.harness.yaml.core.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@FieldNameConstants(innerTypeName = "PipelineExecutionSummaryKeys")
@Entity(value = "pipelineExecutionSummary", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("pipelineExecutionSummary")
@CdUniqueIndex(name = "unique_accountIdentifier_organizationIdentifier_projectIdentifier_identifier_planExecutionId",
    fields =
    {
      @Field(PipelineExecutionSummaryKeys.accountIdentifier)
      , @Field(PipelineExecutionSummaryKeys.orgIdentifier), @Field(PipelineExecutionSummaryKeys.projectIdentifier),
          @Field(PipelineExecutionSummaryKeys.planExecutionId)
    })
public class PipelineExecutionSummary implements PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id String id;

  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;

  private String pipelineIdentifier;
  private String pipelineName;
  private String planExecutionId;
  private ExecutionStatus executionStatus;
  private Long startedAt;
  private Long endedAt;
  private EmbeddedUser triggeredBy;
  private TriggerType triggerType;
  private List<Tag> tags;
  @Builder.Default private List<StageExecutionSummary> stageExecutionSummarySummaryElements = new ArrayList<>();
  @Builder.Default private List<String> stageIdentifiers = new ArrayList<>();
  @Builder.Default private List<String> serviceIdentifiers = new ArrayList<>();
  @Builder.Default private List<String> envIdentifiers = new ArrayList<>();
  @Builder.Default private List<String> serviceDefinitionTypes = new ArrayList<>();
  @Builder.Default private List<NGStageType> stageTypes = new ArrayList<>();

  public void addStageExecutionSummaryElement(StageExecutionSummary stageExecutionSummary) {
    stageExecutionSummarySummaryElements.add(stageExecutionSummary);
  }

  public void addStageIdentifier(String stageIdentifier) {
    stageIdentifiers.add(stageIdentifier);
  }

  public void addEnvironmentIdentifier(String environmentIdentifiers) {
    this.envIdentifiers.add(environmentIdentifiers);
  }

  public void addServiceDefinitionType(String serviceDefinitionTypes) {
    this.serviceDefinitionTypes.add(serviceDefinitionTypes);
  }

  public void addServiceIdentifier(String serviceIdentifiers) {
    this.serviceIdentifiers.add(serviceIdentifiers);
  }

  public void addNGStageType(NGStageType stageType) {
    stageTypes.add(stageType);
  }
}
