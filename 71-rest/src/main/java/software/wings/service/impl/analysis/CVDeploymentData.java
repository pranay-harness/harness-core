package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import software.wings.sm.ExecutionStatus;

/**
 * Created by Praveen
 */
@Data
@Builder
@AllArgsConstructor
public class CVDeploymentData {
  String appId;
  String envId;
  String serviceId;
  String accountId;
  ExecutionStatus status;
  long startTs;
  String workflowExecutionId;
  String pipelineExecutionId;

  public CVDeploymentData(ContinuousVerificationExecutionMetaData cvMetadata) {
    this.accountId = cvMetadata.getAccountId();
    this.appId = cvMetadata.getApplicationId();
    this.workflowExecutionId = cvMetadata.getWorkflowExecutionId();
    this.pipelineExecutionId = cvMetadata.getPipelineExecutionId();
    this.serviceId = cvMetadata.getServiceId();
    this.envId = cvMetadata.getEnvId();
    this.startTs = cvMetadata.getWorkflowStartTs();
  }
}
