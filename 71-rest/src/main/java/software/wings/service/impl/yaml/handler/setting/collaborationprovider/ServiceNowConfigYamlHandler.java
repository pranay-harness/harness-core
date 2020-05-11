package software.wings.service.impl.yaml.handler.setting.collaborationprovider;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.JiraConfig.Yaml;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@OwnedBy(CDC)
public class ServiceNowConfigYamlHandler extends CollaborationProviderYamlHandler<Yaml, ServiceNowConfig> {
  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    final Yaml yaml = changeContext.getYaml();
    final ServiceNowConfig config = new ServiceNowConfig();
    config.setBaseUrl(yaml.getBaseUrl());
    config.setUsername(yaml.getUsername());
    config.setPassword(yaml.getPassword().toCharArray());

    final String accountId = changeContext.getChange().getAccountId();
    config.setAccountId(accountId);

    final String uuid = previous == null ? null : previous.getUuid();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    final ServiceNowConfig jiraConfig = (ServiceNowConfig) settingAttribute.getValue();
    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(jiraConfig.getType())
                    .baseUrl(jiraConfig.getBaseUrl())
                    .username(jiraConfig.getUsername())
                    .password(getEncryptedValue(jiraConfig, "password", false))
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return ServiceNowConfig.Yaml.class;
  }
}
