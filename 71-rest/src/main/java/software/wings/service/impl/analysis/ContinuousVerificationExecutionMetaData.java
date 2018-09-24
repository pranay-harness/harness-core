package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

@Entity(value = "cvExecutionData", noClassnameStored = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class ContinuousVerificationExecutionMetaData extends Base {
  @NotEmpty @Indexed private long workflowStartTs;
  @NotEmpty @Indexed private long pipelineStartTs;

  @NotEmpty @Indexed private String accountId;
  @NotEmpty @Indexed private String envId;
  @NotEmpty @Indexed private String applicationId;
  @NotEmpty @Indexed private String serviceId;
  @NotEmpty @Indexed private String workflowId;
  @NotEmpty @Indexed private String workflowExecutionId;
  @NotEmpty @Indexed private String stateExecutionId;
  @NotEmpty @Indexed private StateType stateType;
  @Indexed private String pipelineId;
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
}
