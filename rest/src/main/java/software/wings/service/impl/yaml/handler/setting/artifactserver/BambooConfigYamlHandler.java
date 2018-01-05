package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.BambooConfig;
import software.wings.beans.BambooConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class BambooConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, BambooConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    BambooConfig bambooConfig = (BambooConfig) settingAttribute.getValue();
    return new Yaml(bambooConfig.getType(), bambooConfig.getBambooUrl(), bambooConfig.getUsername(),
        getEncryptedValue(bambooConfig, "password", false));
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    BambooConfig config = BambooConfig.builder()
                              .accountId(accountId)
                              .bambooUrl(yaml.getUrl())
                              .encryptedPassword(yaml.getPassword())
                              .username(yaml.getUsername())
                              .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
