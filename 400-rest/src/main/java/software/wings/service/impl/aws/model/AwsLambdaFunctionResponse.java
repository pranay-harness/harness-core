package software.wings.service.impl.aws.model;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateMetaInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Created by Pranjal on 01/29/2019
 */
@Data
@Builder
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
public class AwsLambdaFunctionResponse implements AwsResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private ExecutionStatus executionStatus;
  private String errorMessage;
  private List<String> lambdaFunctions;
}
