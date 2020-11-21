package software.wings.service.impl.analysis;

import static software.wings.common.VerificationConstants.ML_RECORDS_TTL_MONTHS;

import io.harness.annotation.HarnessEntity;
import io.harness.beans.ExecutionStatus;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.IndexType;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;
import software.wings.sm.StateType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import java.time.OffsetDateTime;
import java.util.Date;
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

@CdIndex(name = "stateHostIdx",
    fields =
    {
      @Field("workflowId")
      , @Field("stateType"), @Field("executionStatus"), @Field(value = "workflowStartTs", type = IndexType.DESC)
    })
@CdIndex(name = "workflowExec_idx",
    fields = { @Field("workflowExecutionId")
               , @Field(value = "createdAt", type = IndexType.DESC) })
@CdIndex(name = "cv_certified_index", fields = { @Field("pipelineExecutionId")
                                                 , @Field(value = "accountId") })
@Entity(value = "cvExecutionData", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class ContinuousVerificationExecutionMetaData extends Base implements AccountAccess {
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
