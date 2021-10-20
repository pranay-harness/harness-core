package software.wings.service.impl.analysis;

import static software.wings.common.VerificationConstants.ML_RECORDS_TTL_MONTHS;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.ExecutionStatus;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;
import software.wings.sm.StateType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

@Data
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
@FieldNameConstants(innerTypeName = "ContinuousVerificationExecutionMetaDataKeys")
@Entity(value = "cvExecutionData", noClassnameStored = true)
@HarnessEntity(exportable = false)
public final class ContinuousVerificationExecutionMetaData extends Base implements AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(SortCompoundMongoIndex.builder()
                 .name("stateHostIdx")
                 .field(ContinuousVerificationExecutionMetaDataKeys.workflowId)
                 .field(ContinuousVerificationExecutionMetaDataKeys.stateType)
                 .field(ContinuousVerificationExecutionMetaDataKeys.executionStatus)
                 .descSortField(ContinuousVerificationExecutionMetaDataKeys.workflowStartTs)
                 .build(),
            SortCompoundMongoIndex.builder()
                .name("workflowExec_idx")
                .field(ContinuousVerificationExecutionMetaDataKeys.workflowExecutionId)
                .descSortField(CREATED_AT_KEY)
                .build(),
            CompoundMongoIndex.builder()
                .name("cv_certified_index")
                .field(ContinuousVerificationExecutionMetaDataKeys.pipelineExecutionId)
                .field(ContinuousVerificationExecutionMetaDataKeys.accountId)
                .build())
        .build();
  }
  @NotEmpty @FdIndex private long workflowStartTs;
  @NotEmpty private long pipelineStartTs;

  @NotEmpty @FdIndex private String accountId;
  @NotEmpty private String envId;
  @NotEmpty @FdIndex private String applicationId;
  @NotEmpty private String serviceId;
  @NotEmpty private String workflowId;
  @NotEmpty private String workflowExecutionId;
  @NotEmpty @FdIndex private String stateExecutionId;
  @NotEmpty private StateType stateType;
  private String pipelineId;
  @NotEmpty private String pipelineExecutionId;
  @NotEmpty private String phaseId;

  @NotEmpty private String artifactName;
  @NotEmpty private String envName;
  @NotEmpty private String workflowName;
  @NotEmpty private String appName;
  @NotEmpty private String serviceName;
  @NotEmpty private String phaseName;
  @NotEmpty private long stateStartTs;

  private String pipelineName;
  private ExecutionStatus executionStatus;
  private boolean noData;
  private boolean manualOverride;

  @Default
  @JsonIgnore
  @SchemaIgnore
  @FdTtlIndex
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());
}
