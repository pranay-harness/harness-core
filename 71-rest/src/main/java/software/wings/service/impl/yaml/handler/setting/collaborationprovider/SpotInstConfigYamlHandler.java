package software.wings.service.impl.yaml.handler.setting.collaborationprovider;

import com.google.inject.Singleton;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.SpotInstConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@Singleton
public class SpotInstConfigYamlHandler extends CollaborationProviderYamlHandler<Yaml, SpotInstConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SpotInstConfig spotInstConfig = (SpotInstConfig) settingAttribute.getValue();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .spotInstAccountId(spotInstConfig.getSpotInstAccountId())
                    .spotInstToken(getEncryptedValue(spotInstConfig, "spotInstToken", false))
                    .type(spotInstConfig.getType())
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    SpotInstConfig spotInstConfig = SpotInstConfig.builder()
                                        .accountId(accountId)
                                        .spotInstAccountId(yaml.getSpotInstAccountId())
                                        .encryptedSpotInstToken(yaml.getSpotInstToken())
                                        .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, spotInstConfig);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}