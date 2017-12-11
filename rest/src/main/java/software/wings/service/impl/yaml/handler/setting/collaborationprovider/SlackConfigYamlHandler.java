package software.wings.service.impl.yaml.handler.setting.collaborationprovider;

import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.SlackConfig;
import software.wings.beans.SlackConfig.Yaml;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class SlackConfigYamlHandler extends CollaborationProviderYamlHandler<Yaml, SlackConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SlackConfig slackConfig = (SlackConfig) settingAttribute.getValue();
    return new Yaml(slackConfig.getType(), settingAttribute.getName(), slackConfig.getOutgoingWebhookUrl());
  }

  protected SettingAttribute setWithYamlValues(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SlackConfig config = new SlackConfig();
    config.setOutgoingWebhookUrl(yaml.getOutgoingWebhookUrl());
    return buildSettingAttribute(accountId, yaml.getName(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
