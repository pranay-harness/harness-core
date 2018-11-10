package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import com.google.inject.Singleton;

import software.wings.beans.AwsConfig;
import software.wings.beans.AwsConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class AwsConfigYamlHandler extends CloudProviderYamlHandler<Yaml, AwsConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .accessKey(awsConfig.getAccessKey())
        .secretKey(getEncryptedValue(awsConfig, "secretKey", false))
        .type(awsConfig.getType())
        .useEc2IamCredentials(awsConfig.isUseEc2IamCredentials())
        .build();
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    AwsConfig config = AwsConfig.builder()
                           .accountId(accountId)
                           .accessKey(yaml.getAccessKey())
                           .encryptedSecretKey(yaml.getSecretKey())
                           .useEc2IamCredentials(yaml.isUseEc2IamCredentials())
                           .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
