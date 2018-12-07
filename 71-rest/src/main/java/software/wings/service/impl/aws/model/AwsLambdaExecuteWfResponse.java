package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsLambdaExecuteWfResponse extends AwsResponse {
  private String region;
  private AwsConfig awsConfig;
  private List<AwsLambdaFunctionResult> functionResults;

  @Builder
  public AwsLambdaExecuteWfResponse(ExecutionStatus executionStatus, String errorMessage,
      List<AwsLambdaFunctionResult> functionResults, String region, AwsConfig awsConfig) {
    super(executionStatus, errorMessage);
    this.functionResults = functionResults;
    this.awsConfig = awsConfig;
    this.region = region;
  }
}