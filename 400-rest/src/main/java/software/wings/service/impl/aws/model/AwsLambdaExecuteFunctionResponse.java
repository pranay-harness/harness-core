package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;

import software.wings.api.AwsLambdaExecutionData;
import software.wings.beans.LambdaTestEvent;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class AwsLambdaExecuteFunctionResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private Integer statusCode;
  private String functionError;
  private String logResult;
  private String payload;
  private AwsLambdaExecutionData awsLambdaExecutionData;
  private LambdaTestEvent lambdaTestEvent;
}
