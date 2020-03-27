package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import com.google.inject.Singleton;

import software.wings.beans.PcfConfig;
import software.wings.beans.PcfConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@Singleton
public class PcfConfigYamlHandler extends CloudProviderYamlHandler<Yaml, PcfConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .username(pcfConfig.getUsername())
                    .endpointUrl(pcfConfig.getEndpointUrl())
                    .password(getEncryptedValue(pcfConfig, "password", false))
                    .type(pcfConfig.getType())
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

    PcfConfig config = PcfConfig.builder()
                           .accountId(accountId)
                           .username(yaml.getUsername())
                           .encryptedPassword(yaml.getPassword())
                           .endpointUrl(yaml.getEndpointUrl())
                           .build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
