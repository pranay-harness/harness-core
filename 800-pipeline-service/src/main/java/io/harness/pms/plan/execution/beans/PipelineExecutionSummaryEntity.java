package io.harness.pms.plan.execution.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.pms.contracts.execution.ExecutionErrorInfo;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.plan.execution.beans.dto.GraphLayoutNodeDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "PlanExecutionSummaryKeys")
@Entity(value = "planExecutionsSummary", noClassnameStored = true)
@Document("planExecutionsSummary")
@TypeAlias("planExecutionsSummary")
@HarnessEntity(exportable = true)
public class PipelineExecutionSummaryEntity implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id @org.mongodb.morphia.annotations.Id private String uuid;

  @NotEmpty int runSequence;
  @NotEmpty String accountId;
  @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;

  @NotEmpty @FdUniqueIndex private String pipelineIdentifier;
  @NotEmpty @FdUniqueIndex private String planExecutionId;
  @NotEmpty @FdUniqueIndex private String name;

  private ExecutionStatus status;

  private String inputSetYaml;
  private Map<String, String> tags;

  @Builder.Default private Map<String, org.bson.Document> moduleInfo = new HashMap<>();
  @Builder.Default private Map<String, GraphLayoutNodeDTO> layoutNodeMap = new HashMap<>();
  private String startingNodeId;

  private ExecutionTriggerInfo executionTriggerInfo;
  private ExecutionErrorInfo executionErrorInfo;

  private Long startTs;
  private Long endTs;

  @SchemaIgnore @FdIndex @CreatedDate private long createdAt;
  @SchemaIgnore @NotNull @LastModifiedDate private long lastUpdatedAt;
  @Version private Long version;
}
