package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;
import software.wings.sm.StateType;

@Entity(value = "cvExecutionData", noClassnameStored = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class CVExecutionMetaData extends Base {
  @NotEmpty @Indexed private long workflowStartTs;
  @NotEmpty @Indexed private long stateStartTs;
  @NotEmpty @Indexed private long pipelineStartTs;

  @NotEmpty @Indexed private String applicationId;
  @NotEmpty @Indexed private String serviceId;
  @NotEmpty @Indexed private String workflowId;
  @NotEmpty @Indexed private String workflowExecutionId;
  @NotEmpty @Indexed private String stateExecutionId;
  @NotEmpty @Indexed private String artifactName;
  @NotEmpty @Indexed private String envName;
  @NotEmpty @Indexed private String workflowName;
  @NotEmpty @Indexed private String appName;
  @NotEmpty @Indexed private String serviceName;
  @NotEmpty @Indexed private String phaseName;
  @NotEmpty @Indexed private String pipelineName;
  @NotEmpty @Indexed private StateType stateType;
  @NotEmpty @Indexed private String accountId;
}
