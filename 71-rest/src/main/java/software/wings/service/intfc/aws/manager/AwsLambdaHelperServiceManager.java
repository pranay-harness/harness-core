package software.wings.service.intfc.aws.manager;

import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

/**
 * Created by Pranjal on 01/29/2019
 */
public interface AwsLambdaHelperServiceManager {
  List<String> listLambdaFunctions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region);
}
