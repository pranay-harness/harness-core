package software.wings.service.impl;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.AccountPlugin.Builder.anAccountPlugin;
import static software.wings.beans.PluginCategory.Artifact;
import static software.wings.beans.PluginCategory.CloudProvider;
import static software.wings.beans.PluginCategory.Collaboration;
import static software.wings.beans.PluginCategory.Verification;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.inject.Singleton;

import software.wings.beans.AccountPlugin;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AwsConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SlackConfig;
import software.wings.beans.SplunkConfig;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.PluginService;
import software.wings.utils.JsonUtils;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 10/20/16.
 */
@Singleton
public class PluginServiceImpl implements PluginService {
  private static final String stencilsPath = "/templates/plugins/";
  private static final String uiSchemaSuffix = "-SettingUISchema.json";

  @Override
  public List<AccountPlugin> getInstalledPlugins(String accountId) {
    return Lists.newArrayList(anAccountPlugin()
                                  .withSettingClass(JenkinsConfig.class)
                                  .withAccountId(accountId)
                                  .withIsEnabled(true)
                                  .withDisplayName("Jenkins")
                                  .withType("JENKINS")
                                  .withPluginCategories(asList(Verification, Artifact))
                                  .withUiSchema(readUiSchema("JENKINS"))
                                  .build(),
        anAccountPlugin()
            .withSettingClass(BambooConfig.class)
            .withAccountId(accountId)
            .withIsEnabled(true)
            .withDisplayName("Bamboo")
            .withType("BAMBOO")
            .withPluginCategories(asList(Artifact))
            .withUiSchema(readUiSchema("BAMBOO"))
            .build(),
        anAccountPlugin()
            .withSettingClass(DockerConfig.class)
            .withAccountId(accountId)
            .withIsEnabled(true)
            .withDisplayName("Docker Registry")
            .withType("DOCKER_REGISTRY")
            .withPluginCategories(asList(Artifact))
            .withUiSchema(readUiSchema("DOCKER_REGISTRY"))
            .build(),
        anAccountPlugin()
            .withSettingClass(AppDynamicsConfig.class)
            .withAccountId(accountId)
            .withIsEnabled(true)
            .withDisplayName("AppDynamics")
            .withType("APP_DYNAMICS")
            .withPluginCategories(asList(Verification))
            .withUiSchema(readUiSchema("APP_DYNAMICS"))
            .build(),
        anAccountPlugin()
            .withSettingClass(SplunkConfig.class)
            .withAccountId(accountId)
            .withIsEnabled(true)
            .withDisplayName("Splunk")
            .withType("SPLUNK")
            .withPluginCategories(asList(Verification))
            .withUiSchema(readUiSchema("SPLUNK"))
            .build(),
        anAccountPlugin()
            .withSettingClass(SmtpConfig.class)
            .withAccountId(accountId)
            .withIsEnabled(true)
            .withDisplayName("SMTP")
            .withType("SMTP")
            .withPluginCategories(asList(Collaboration))
            .withUiSchema(readUiSchema("SMTP"))
            .build(),
        anAccountPlugin()
            .withSettingClass(SlackConfig.class)
            .withAccountId(accountId)
            .withIsEnabled(true)
            .withDisplayName("SLACK")
            .withType("SLACK")
            .withPluginCategories(asList(Collaboration))
            .withUiSchema(readUiSchema("SLACK"))
            .build(),
        anAccountPlugin()
            .withSettingClass(AwsConfig.class)
            .withAccountId(accountId)
            .withIsEnabled(true)
            .withDisplayName("AWS")
            .withType("AWS")
            .withPluginCategories(asList(CloudProvider))
            .withUiSchema(readUiSchema("AWS"))
            .build());
  }

  @Override
  public Map<String, Map<String, Object>> getPluginSettingSchema(String accountId) {
    return getInstalledPlugins(accountId)
        .stream()
        .filter(accountPlugin -> accountPlugin.getSettingClass() != null)
        .collect(toMap(AccountPlugin::getType,
            accountPlugin
            -> ImmutableMap.of("jsonSchema", JsonUtils.jsonSchema(accountPlugin.getSettingClass()), "uiSchema",
                accountPlugin.getUiSchema())));
  }

  private Object readUiSchema(String type) {
    try {
      return readResource(stencilsPath + type + uiSchemaSuffix);
    } catch (Exception e) {
      return new HashMap<String, Object>();
    }
  }

  private Object readResource(String file) {
    try {
      URL url = this.getClass().getResource(file);
      String json = Resources.toString(url, Charsets.UTF_8);
      return JsonUtils.asObject(json, HashMap.class);
    } catch (Exception exception) {
      throw new WingsException("Error in reasing ui schema - " + file, exception);
    }
  }
}
