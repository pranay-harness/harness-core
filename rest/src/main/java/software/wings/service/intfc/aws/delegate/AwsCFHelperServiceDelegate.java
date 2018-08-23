package software.wings.service.intfc.aws.delegate;

import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;

import java.util.List;

public interface AwsCFHelperServiceDelegate {
  String getStackBody(AwsConfig awsConfig, String region, String stackId);
  List<AwsCFTemplateParamsData> getParamsData(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String data, String type);
}
