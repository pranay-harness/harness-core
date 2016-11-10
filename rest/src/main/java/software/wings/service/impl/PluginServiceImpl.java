package software.wings.service.impl;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.AccountPlugin.Builder.anAccountPlugin;
import static software.wings.beans.PluginCategory.Artifact;
import static software.wings.beans.PluginCategory.Collaboration;
import static software.wings.beans.PluginCategory.Verification;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import software.wings.beans.AccountPlugin;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SplunkConfig;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.PluginService;
import software.wings.utils.JsonUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 10/20/16.
 */
@Singleton
public class PluginServiceImpl implements PluginService {
  @Override
  public List<AccountPlugin> getInstalledPlugins(String accountId) {
    return Lists.newArrayList(anAccountPlugin()
                                  .withSettingClass(JenkinsConfig.class)
                                  .withAccountId(accountId)
                                  .withIsEnabled(true)
                                  .withDisplayName("Jenkins")
                                  .withType("JENKINS")
                                  .withPluginCategories(asList(Verification, Artifact))
                                  .build(),
        anAccountPlugin()
            .withSettingClass(AppDynamicsConfig.class)
            .withAccountId(accountId)
            .withIsEnabled(true)
            .withDisplayName("AppDynamics")
            .withType("APP_DYNAMICS")
            .withPluginCategories(asList(Verification))
            .build(),
        anAccountPlugin()
            .withSettingClass(SplunkConfig.class)
            .withAccountId(accountId)
            .withIsEnabled(true)
            .withDisplayName("Splunk")
            .withType("SPLUNK")
            .withPluginCategories(asList(Verification))
            .build(),
        anAccountPlugin()
            .withSettingClass(SmtpConfig.class)
            .withAccountId(accountId)
            .withIsEnabled(true)
            .withDisplayName("SMTP")
            .withType("SMTP")
            .withPluginCategories(asList(Collaboration))
            .build());
  }

  @Override
  public Map<String, JsonNode> getPluginSettingSchema(String accountId) {
    return getInstalledPlugins(accountId)
        .stream()
        .filter(accountPlugin -> accountPlugin.getSettingClass() != null)
        .collect(toMap(AccountPlugin::getType, accountPlugin -> JsonUtils.jsonSchema(accountPlugin.getSettingClass())));
  }
}
