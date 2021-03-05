package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SmbConfig;
import software.wings.beans.SmbConfigYaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@OwnedBy(CDC)
public class SmbConfigYamlHandler extends ArtifactServerYamlHandler<SmbConfigYaml, SmbConfig> {
  @Override
  public SmbConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    SmbConfig smbConfig = (SmbConfig) settingAttribute.getValue();
    SmbConfigYaml yaml = SmbConfigYaml.builder()
                             .harnessApiVersion(getHarnessApiVersion())
                             .type(smbConfig.getType())
                             .url(smbConfig.getSmbUrl())
                             .username(smbConfig.getUsername())
                             .password(getEncryptedYamlRef(smbConfig.getAccountId(), smbConfig.getEncryptedPassword()))
                             .domain(smbConfig.getDomain())
                             .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<SmbConfigYaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    SmbConfigYaml yaml = changeContext.getYaml();
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
    return SmbConfigYaml.class;
  }
}
