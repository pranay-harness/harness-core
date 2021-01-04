package software.wings.service.intfc.aws.manager;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.embed.AwsLambdaDetails;
import software.wings.service.impl.aws.model.request.AwsLambdaDetailsRequest;

import java.util.List;

/**
 * Created by Pranjal on 01/29/2019
 */
public interface AwsLambdaHelperServiceManager {
  List<String> listLambdaFunctions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
  AwsLambdaDetails getFunctionDetails(AwsLambdaDetailsRequest request);
}
