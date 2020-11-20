package software.wings.service.intfc.aws.delegate;

import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.AwsConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;

import java.util.List;

public interface AwsCFHelperServiceDelegate {
  String getStackBody(AwsConfig awsConfig, String region, String stackId);
  List<AwsCFTemplateParamsData> getParamsData(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String data, String type, GitFileConfig gitFileConfig, GitConfig gitConfig,
      List<EncryptedDataDetail> sourceRepoEncryptedDetail);
  List<String> getCapabilities(AwsConfig awsConfig, String region, String data, String type);
}
