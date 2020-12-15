package io.harness.ngpipeline.pipeline.executions.beans;

import io.harness.mongo.index.Field;
import io.harness.mongo.index.NgUniqueIndex;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ngpipeline.pipeline.executions.beans.PipelineExecutionSummary.PipelineExecutionSummaryKeys;
import io.harness.persistence.PersistentEntity;
import io.harness.pipeline.executions.NGStageType;
import io.harness.pms.contracts.execution.ExecutionErrorInfo;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.pipeline.ExecutionTriggerInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "PipelineExecutionSummaryKeys")
@Entity(value = "pipelineExecutionSummary", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document("pipelineExecutionSummary")
@NgUniqueIndex(name = "unique_accountIdentifier_organizationIdentifier_projectIdentifier_identifier_planExecutionId",
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
  private ExecutionTriggerInfo triggerInfo;
  private List<NGTag> tags;
  private ExecutionErrorInfo errorInfo;
  private String inputSetYaml;
  @Builder.Default private List<StageExecutionSummary> stageExecutionSummarySummaryElements = new ArrayList<>();
  @Builder.Default private List<String> stageIdentifiers = new ArrayList<>();
  @Builder.Default private List<String> serviceIdentifiers = new ArrayList<>();
  @Builder.Default private List<String> envIdentifiers = new ArrayList<>();
  @Builder.Default private List<String> serviceDefinitionTypes = new ArrayList<>();
  @Builder.Default private List<NGStageType> stageTypes = new ArrayList<>();
  @Builder.Default private List<EnvironmentType> environmentTypes = new ArrayList<>();

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
