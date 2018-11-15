package software.wings.service.impl.yaml.handler.setting.collaborationprovider;
import com.google.inject.Singleton;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.JiraConfig;
import software.wings.beans.JiraConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;
/**
 * Converstion between bean <-and-> yaml.
 *
 * @author swagat on 9/6/18
 *
 *
 */
@Singleton
public class JiraConfigYamlHandler extends CollaborationProviderYamlHandler<Yaml, JiraConfig> {
  @Override
  protected SettingAttribute toBean(final SettingAttribute previous, final ChangeContext<Yaml> changeContext,
      final List<ChangeContext> changeSetContext) throws HarnessException {
    final Yaml yaml = changeContext.getYaml();
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
  public Yaml toYaml(final SettingAttribute settingAttribute, final String appId) {
    final JiraConfig jiraConfig = (JiraConfig) settingAttribute.getValue();
    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(jiraConfig.getType())
        .baseUrl(jiraConfig.getBaseUrl())
        .username(jiraConfig.getUsername())
        .password(getEncryptedValue((EncryptableSetting) jiraConfig, "password", false))
        .build();
  }

  @Override
  public Class getYamlClass() {
    return JiraConfig.Yaml.class;
  }
}