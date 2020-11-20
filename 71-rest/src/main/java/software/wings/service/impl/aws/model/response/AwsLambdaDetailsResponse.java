package software.wings.service.impl.aws.model.response;

import com.amazonaws.services.lambda.model.GetFunctionResult;
import com.amazonaws.services.lambda.model.ListAliasesResult;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;
import lombok.Builder;
import lombok.Data;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.impl.aws.model.embed.AwsLambdaDetails;

@Data
public class AwsLambdaDetailsResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private AwsLambdaDetails lambdaDetails;

  @Builder
  public AwsLambdaDetailsResponse(DelegateMetaInfo delegateMetaInfo, ExecutionStatus executionStatus,
      String errorMessage, AwsLambdaDetails details) {
    this.delegateMetaInfo = delegateMetaInfo;
    this.executionStatus = executionStatus;
    this.errorMessage = errorMessage;
    this.lambdaDetails = details;
  }

  public static AwsLambdaDetailsResponse from(GetFunctionResult result, ListAliasesResult listAliasesResult) {
    return AwsLambdaDetailsResponse.builder()
        .executionStatus(ExecutionStatus.SUCCESS)
        .details(AwsLambdaDetails.from(result, listAliasesResult))
        .build();
  }
}
