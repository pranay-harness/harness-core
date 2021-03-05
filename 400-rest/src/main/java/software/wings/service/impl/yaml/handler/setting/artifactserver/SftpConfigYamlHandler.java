package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SftpConfig;
import software.wings.beans.SftpConfigYaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@OwnedBy(CDC)
public class SftpConfigYamlHandler extends ArtifactServerYamlHandler<SftpConfigYaml, SftpConfig> {
  @Override
  public SftpConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    SftpConfig sftpConfig = (SftpConfig) settingAttribute.getValue();
    SftpConfigYaml yaml =
        SftpConfigYaml.builder()
            .harnessApiVersion(getHarnessApiVersion())
            .type(sftpConfig.getType())
            .url(sftpConfig.getSftpUrl())
            .username(sftpConfig.getUsername())
            .password(getEncryptedYamlRef(sftpConfig.getAccountId(), sftpConfig.getEncryptedPassword()))
            .domain(sftpConfig.getDomain())
            .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<SftpConfigYaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    SftpConfigYaml yaml = changeContext.getYaml();
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
    return SftpConfigYaml.class;
  }
}
