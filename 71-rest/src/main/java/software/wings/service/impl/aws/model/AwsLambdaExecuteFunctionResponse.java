package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import lombok.Builder;
import lombok.Data;
import software.wings.api.AwsLambdaExecutionData;
import software.wings.beans.LambdaTestEvent;

@Data
@Builder
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