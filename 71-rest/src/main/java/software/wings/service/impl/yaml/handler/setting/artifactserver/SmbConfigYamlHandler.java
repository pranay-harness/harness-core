package software.wings.service.impl.yaml.handler.setting.artifactserver;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SmbConfig;
import software.wings.beans.SmbConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class SmbConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, SmbConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SmbConfig smbConfig = (SmbConfig) settingAttribute.getValue();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(smbConfig.getType())
                    .url(smbConfig.getSmbUrl())
                    .username(smbConfig.getUsername())
                    .password(getEncryptedValue(smbConfig, "password", false))
                    .domain(smbConfig.getDomain())
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

    SmbConfig config = SmbConfig.builder()
                           .accountId(accountId)
                           .smbUrl(yaml.getUrl())
                           .username(yaml.getUsername())
                           .encryptedPassword(yaml.getPassword())
                           .domain(yaml.getDomain())
                           .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
