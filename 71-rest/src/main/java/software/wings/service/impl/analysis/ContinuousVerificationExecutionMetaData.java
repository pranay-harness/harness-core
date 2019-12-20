package software.wings.service.impl.analysis;

import static software.wings.common.VerificationConstants.ML_RECORDS_TTL_MONTHS;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.utils.IndexType;
import software.wings.beans.Base;
import software.wings.sm.StateType;

import java.time.OffsetDateTime;
import java.util.Date;

@Data
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
@FieldNameConstants(innerTypeName = "ContinuousVerificationExecutionMetaDataKeys")
@Indexes({
  @Index(fields =
      {
        @Field("workflowId")
        , @Field("stateType"), @Field("executionStatus"), @Field(value = "workflowStartTs", type = IndexType.DESC)
      },
      options = @IndexOptions(name = "stateHostIdx"))
  ,
      @Index(fields = {
        @Field("workflowExecutionId"), @Field(value = "createdAt", type = IndexType.DESC)
      }, options = @IndexOptions(name = "workflowExec_idx")), @Index(fields = {
        @Field("pipelineExecutionId"), @Field(value = "accountId")
      }, options = @IndexOptions(name = "cv_certified_index"))
})
@Entity(value = "cvExecutionData", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class ContinuousVerificationExecutionMetaData extends Base {
  @NotEmpty @Indexed private long workflowStartTs;
  @NotEmpty private long pipelineStartTs;

  @NotEmpty @Indexed private String accountId;
  @NotEmpty private String envId;
  @NotEmpty @Indexed private String applicationId;
  @NotEmpty private String serviceId;
  @NotEmpty private String workflowId;
  @NotEmpty private String workflowExecutionId;
  @NotEmpty @Indexed private String stateExecutionId;
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
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(ML_RECORDS_TTL_MONTHS).toInstant());
}
