package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import io.harness.exception.InvalidRequestException;

import software.wings.beans.AwsConfig;
import software.wings.beans.AwsConfigYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class AwsConfigYamlHandler extends CloudProviderYamlHandler<AwsConfigYaml, AwsConfig> {
  @Override
  public AwsConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    String secretValueYamlRef = isNotEmpty(awsConfig.getEncryptedSecretKey())
        ? getEncryptedYamlRef(awsConfig.getAccountId(), awsConfig.getEncryptedSecretKey())
        : null;
    boolean useEncryptedAccessKey = awsConfig.isUseEncryptedAccessKey();
    AwsConfigYaml yaml = AwsConfigYaml.builder()
                             .harnessApiVersion(getHarnessApiVersion())
                             .accessKey(getAccessKey(awsConfig))
                             .accessKeySecretId(useEncryptedAccessKey
                                     ? getEncryptedYamlRef(awsConfig.getAccountId(), awsConfig.getEncryptedAccessKey())
                                     : null)
                             .secretKey(secretValueYamlRef)
                             .type(awsConfig.getType())
                             .useEc2IamCredentials(awsConfig.isUseEc2IamCredentials())
                             .assumeCrossAccountRole(awsConfig.isAssumeCrossAccountRole())
                             .crossAccountAttributes(awsConfig.getCrossAccountAttributes())
                             .tag(awsConfig.getTag())
                             .defaultRegion(awsConfig.getDefaultRegion())
                             .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  private String getAccessKey(AwsConfig awsConfig) {
    if (awsConfig.getAccessKey() == null || awsConfig.isUseEncryptedAccessKey()) {
      return null;
    }
    return String.valueOf(awsConfig.getAccessKey());
  }

  @Override
  public SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<AwsConfigYaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    AwsConfigYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    if (isNotEmpty(yaml.getAccessKey()) && isNotEmpty(yaml.getAccessKeySecretId())) {
      throw new InvalidRequestException("Cannot set both value and secret reference for accessKey field", USER);
    }

    AwsConfig config = AwsConfig.builder()
                           .accountId(accountId)
                           .accessKey(yaml.getAccessKey() != null ? yaml.getAccessKey().toCharArray() : null)
                           .encryptedAccessKey(yaml.getAccessKeySecretId())
                           .useEncryptedAccessKey(yaml.getAccessKeySecretId() != null)
                           .encryptedSecretKey(yaml.getSecretKey())
                           .useEc2IamCredentials(yaml.isUseEc2IamCredentials())
                           .tag(yaml.getTag())
                           .assumeCrossAccountRole(yaml.isAssumeCrossAccountRole())
                           .crossAccountAttributes(yaml.getCrossAccountAttributes())
                           .defaultRegion(yaml.getDefaultRegion())
                           .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return AwsConfigYaml.class;
  }
}
