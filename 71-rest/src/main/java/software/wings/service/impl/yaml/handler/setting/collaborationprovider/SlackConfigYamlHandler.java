package software.wings.service.impl.yaml.handler.setting.collaborationprovider;

import com.google.inject.Singleton;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SlackConfig;
import software.wings.beans.SlackConfig.Yaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class SlackConfigYamlHandler extends CollaborationProviderYamlHandler<Yaml, SlackConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    SlackConfig slackConfig = (SlackConfig) settingAttribute.getValue();

    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(slackConfig.getType())
        .outgoingWebhookUrl(slackConfig.getOutgoingWebhookUrl())
        .build();
  }

  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SlackConfig config = new SlackConfig();
    config.setOutgoingWebhookUrl(yaml.getOutgoingWebhookUrl());
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
