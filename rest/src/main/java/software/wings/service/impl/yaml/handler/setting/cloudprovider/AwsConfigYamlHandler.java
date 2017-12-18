package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import software.wings.beans.AwsConfig;
import software.wings.beans.AwsConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.io.IOException;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class AwsConfigYamlHandler extends CloudProviderYamlHandler<Yaml, AwsConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    return new Yaml(awsConfig.getType(), awsConfig.getAccessKey(), getEncryptedValue(awsConfig, "secretKey", false));
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    char[] decryptedSecretKey;
    try {
      decryptedSecretKey = secretManager.decryptYamlRef(yaml.getSecretKey());
    } catch (IllegalAccessException | IOException e) {
      throw new HarnessException("Exception while decrypting the secret key ref:" + yaml.getSecretKey());
    }

    AwsConfig config = AwsConfig.builder()
                           .accountId(accountId)
                           .accessKey(yaml.getAccessKey())
                           .secretKey(decryptedSecretKey)
                           .encryptedSecretKey(yaml.getSecretKey())
                           .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
