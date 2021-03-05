package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.SettingAttribute;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.config.NexusConfigYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@OwnedBy(CDC)
@Singleton
public class NexusConfigYamlHandler extends ArtifactServerYamlHandler<NexusConfigYaml, NexusConfig> {
  @Override
  public NexusConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    NexusConfig nexusConfig = (NexusConfig) settingAttribute.getValue();
    NexusConfigYaml yaml;
    if (nexusConfig.hasCredentials()) {
      yaml = NexusConfigYaml.builder()
                 .harnessApiVersion(getHarnessApiVersion())
                 .type(nexusConfig.getType())
                 .url(nexusConfig.getNexusUrl())
                 .username(nexusConfig.getUsername())
                 .password(getEncryptedYamlRef(nexusConfig.getAccountId(), nexusConfig.getEncryptedPassword()))
                 .version(nexusConfig.getVersion())
                 .build();
    } else {
      yaml = NexusConfigYaml.builder()
                 .harnessApiVersion(getHarnessApiVersion())
                 .type(nexusConfig.getType())
                 .url(nexusConfig.getNexusUrl())
                 .version(nexusConfig.getVersion())
                 .build();
    }
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<NexusConfigYaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    NexusConfigYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    NexusConfig config = NexusConfig.builder()
                             .accountId(accountId)
                             .nexusUrl(yaml.getUrl())
                             .encryptedPassword(yaml.getPassword())
                             .username(yaml.getUsername())
                             .version(yaml.getVersion())
                             .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return NexusConfigYaml.class;
  }
}
