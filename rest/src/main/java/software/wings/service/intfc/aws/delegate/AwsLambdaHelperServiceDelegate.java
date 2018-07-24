package software.wings.service.intfc.aws.delegate;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionResponse;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfResponse;

public interface AwsLambdaHelperServiceDelegate {
  AwsLambdaExecuteWfResponse executeWf(AwsLambdaExecuteWfRequest request, ExecutionLogCallback logCallback);
  AwsLambdaExecuteFunctionResponse executeFunction(AwsLambdaExecuteFunctionRequest request);
}