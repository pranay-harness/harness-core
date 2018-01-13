package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.SettingAttribute;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.config.NexusConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.io.IOException;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class NexusConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, NexusConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    NexusConfig nexusConfig = (NexusConfig) settingAttribute.getValue();
    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(nexusConfig.getType())
        .url(nexusConfig.getNexusUrl())
        .username(nexusConfig.getUsername())
        .password(getEncryptedValue(nexusConfig, "password", false))
        .build();
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    char[] decryptedPassword;
    try {
      decryptedPassword = secretManager.decryptYamlRef(yaml.getPassword());
    } catch (IllegalAccessException | IOException e) {
      throw new HarnessException("Exception while decrypting the password ref:" + yaml.getPassword());
    }

    NexusConfig config = NexusConfig.builder()
                             .accountId(accountId)
                             .nexusUrl(yaml.getUrl())
                             .password(decryptedPassword)
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
