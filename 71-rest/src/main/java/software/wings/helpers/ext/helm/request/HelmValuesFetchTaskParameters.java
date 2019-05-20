package software.wings.helpers.ext.helm.request;

import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Data;
import software.wings.service.impl.ContainerServiceParams;

@Data
@Builder
public class HelmValuesFetchTaskParameters implements TaskParameters {
  private String accountId;
  private String appId;
  private String activityId;
  private String workflowExecutionId;

  // This is to support helm v1
  private ContainerServiceParams containerServiceParams;
  private String helmCommandFlags;

  private HelmChartConfigParams helmChartConfigTaskParams;
}
