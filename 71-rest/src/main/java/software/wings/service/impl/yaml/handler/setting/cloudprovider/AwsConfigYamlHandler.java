package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Singleton;

import io.harness.exception.HarnessException;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@Singleton
public class AwsConfigYamlHandler extends CloudProviderYamlHandler<Yaml, AwsConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    String secretValueYamlRef =
        isNotEmpty(awsConfig.getEncryptedSecretKey()) ? getEncryptedValue(awsConfig, "secretKey", false) : null;
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .accessKey(awsConfig.getAccessKey())
                    .secretKey(secretValueYamlRef)
                    .type(awsConfig.getType())
                    .useEc2IamCredentials(awsConfig.isUseEc2IamCredentials())
                    .assumeCrossAccountRole(awsConfig.isAssumeCrossAccountRole())
                    .crossAccountAttributes(awsConfig.getCrossAccountAttributes())
                    .tag(awsConfig.getTag())
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
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
                           .tag(yaml.getTag())
                           .assumeCrossAccountRole(yaml.isAssumeCrossAccountRole())
                           .crossAccountAttributes(yaml.getCrossAccountAttributes())
                           .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
