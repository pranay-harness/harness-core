package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.AccountPlugin.Builder.anAccountPlugin;
import static software.wings.beans.PluginCategory.Artifact;
import static software.wings.beans.PluginCategory.CloudProvider;
import static software.wings.beans.PluginCategory.Collaboration;
import static software.wings.beans.PluginCategory.Verification;

import org.junit.Test;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AwsConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.PhysicalDataCenter;
import software.wings.beans.SlackConfig;
import software.wings.beans.SplunkConfig;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.impl.PluginServiceImpl;
import software.wings.service.intfc.PluginService;

/**
 * Created by peeyushaggarwal on 10/21/16.
 */
public class PluginServiceTest {
  private PluginService pluginService = new PluginServiceImpl();

  @Test
  public void shouldGetInstalledPlugins() throws Exception {
    String accountId = "ACCOUNT_ID";

    assertThat(pluginService.getInstalledPlugins(accountId))
        .hasSize(8)
        .containsExactly(anAccountPlugin()
                             .withSettingClass(JenkinsConfig.class)
                             .withAccountId(accountId)
                             .withIsEnabled(true)
                             .withDisplayName("Jenkins")
                             .withType("JENKINS")
                             .withPluginCategories(asList(Verification, Artifact))
                             .build(),
            anAccountPlugin()
                .withSettingClass(BambooConfig.class)
                .withAccountId(accountId)
                .withIsEnabled(true)
                .withDisplayName("Bamboo")
                .withType("BAMBOO")
                .withPluginCategories(asList(Artifact))
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
                .build(),
            anAccountPlugin()
                .withSettingClass(SlackConfig.class)
                .withAccountId(accountId)
                .withIsEnabled(true)
                .withDisplayName("SLACK")
                .withType("SLACK")
                .withPluginCategories(asList(Collaboration))
                .build(),
            anAccountPlugin()
                .withSettingClass(AwsConfig.class)
                .withAccountId(accountId)
                .withIsEnabled(true)
                .withDisplayName("AWS")
                .withType("AWS")
                .withPluginCategories(asList(CloudProvider))
                .build(),
            anAccountPlugin()
                .withSettingClass(PhysicalDataCenter.class)
                .withAccountId(accountId)
                .withIsEnabled(true)
                .withDisplayName("Physical Data Center")
                .withType("PHYSICAL_DATA_CENTER")
                .withPluginCategories(asList(CloudProvider))
                .build());
  }

  @Test
  public void shouldGetPluginSettingSchema() throws Exception {
    String accountId = "ACCOUNT_ID";

    assertThat(pluginService.getPluginSettingSchema(accountId))
        .hasSize(8)
        .containsOnlyKeys(
            "APP_DYNAMICS", "JENKINS", "BAMBOO", "SMTP", "SLACK", "SPLUNK", "AWS", "PHYSICAL_DATA_CENTER");
  }
}
