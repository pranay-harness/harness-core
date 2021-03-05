package software.wings.service.impl.yaml.handler.setting.collaborationprovider;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.HarnessException;

import software.wings.beans.JiraConfig;
import software.wings.beans.JiraConfigYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;
/**
 * Converstion between bean <-and-> yaml.
 *
 * @author swagat on 9/6/18
 *
 *
 */
@OwnedBy(CDC)
@Singleton
public class JiraConfigYamlHandler extends CollaborationProviderYamlHandler<JiraConfigYaml, JiraConfig> {
  @Override
  protected SettingAttribute toBean(final SettingAttribute previous, final ChangeContext<JiraConfigYaml> changeContext,
      final List<ChangeContext> changeSetContext) throws HarnessException {
    final JiraConfigYaml yaml = changeContext.getYaml();
    final JiraConfig config = new JiraConfig();
    config.setBaseUrl(yaml.getBaseUrl());
    config.setUsername(yaml.getUsername());
    config.setPassword(yaml.getPassword().toCharArray());

    final String accountId = changeContext.getChange().getAccountId();
    config.setAccountId(accountId);

    final String uuid = previous == null ? null : previous.getUuid();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public JiraConfigYaml toYaml(final SettingAttribute settingAttribute, final String appId) {
    final JiraConfig jiraConfig = (JiraConfig) settingAttribute.getValue();
    JiraConfigYaml yaml =
        JiraConfigYaml.builder()
            .harnessApiVersion(getHarnessApiVersion())
            .type(jiraConfig.getType())
            .baseUrl(jiraConfig.getBaseUrl())
            .username(jiraConfig.getUsername())
            .password(getEncryptedYamlRef(jiraConfig.getAccountId(), jiraConfig.getEncryptedPassword()))
            .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return JiraConfigYaml.class;
  }
}
