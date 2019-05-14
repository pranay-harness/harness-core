package software.wings.service.impl.yaml.handler.setting.artifactserver;

import io.harness.exception.HarnessException;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SftpConfig;
import software.wings.beans.SftpConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class SftpConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, SftpConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SftpConfig sftpConfig = (SftpConfig) settingAttribute.getValue();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(sftpConfig.getType())
                    .url(sftpConfig.getSftpUrl())
                    .username(sftpConfig.getUsername())
                    .password(getEncryptedValue(sftpConfig, "password", false))
                    .domain(sftpConfig.getDomain())
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    SftpConfig.Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    SftpConfig config = SftpConfig.builder()
                            .accountId(accountId)
                            .sftpUrl(yaml.getUrl())
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
