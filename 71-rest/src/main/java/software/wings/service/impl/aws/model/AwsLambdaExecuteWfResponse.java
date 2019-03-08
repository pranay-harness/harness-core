package software.wings.service.impl.aws.model;

import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.AwsConfig;

import java.util.List;

@Data
@Builder
public class AwsLambdaExecuteWfResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private String region;
  private AwsConfig awsConfig;
  private List<AwsLambdaFunctionResult> functionResults;
}