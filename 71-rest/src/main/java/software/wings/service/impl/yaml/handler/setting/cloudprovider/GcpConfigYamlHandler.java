package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import com.google.inject.Singleton;

import software.wings.beans.GcpConfig;
import software.wings.beans.GcpConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class GcpConfigYamlHandler extends CloudProviderYamlHandler<Yaml, GcpConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    GcpConfig gcpConfig = (GcpConfig) settingAttribute.getValue();

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(gcpConfig.getType())
                    .serviceAccountKeyFileContent(getEncryptedValue(gcpConfig, "serviceAccountKeyFileContent", false))
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    GcpConfig config = GcpConfig.builder()
                           .accountId(accountId)
                           .encryptedServiceAccountKeyFileContent(yaml.getServiceAccountKeyFileContent())
                           .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
