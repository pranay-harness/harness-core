package software.wings.service.intfc.aws.manager;

import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionResponse;

import java.util.List;

public interface AwsLambdaHelperServiceManager {
  AwsLambdaExecuteFunctionResponse executeFunction(AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String region, String functionName, String qualifier, String payload, String appId);
}
