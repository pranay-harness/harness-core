package software.wings.service.intfc.aws.delegate;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceDeployResponse;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupRequest;
import software.wings.service.impl.aws.model.AwsAmiServiceSetupResponse;

public interface AwsAmiHelperServiceDelegate {
  AwsAmiServiceSetupResponse setUpAmiService(AwsAmiServiceSetupRequest request, ExecutionLogCallback logCallback);
  AwsAmiServiceDeployResponse deployAmiService(AwsAmiServiceDeployRequest request, ExecutionLogCallback logCallback);
}