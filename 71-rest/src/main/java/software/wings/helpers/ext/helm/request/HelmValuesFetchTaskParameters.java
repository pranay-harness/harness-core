package software.wings.helpers.ext.helm.request;

import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HelmValuesFetchTaskParameters implements TaskParameters {
  private String accountId;
  private String appId;
  private String activityId;
  private String workflowExecutionId;

  private HelmChartConfigParams helmChartConfigTaskParams;
}
