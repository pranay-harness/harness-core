package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.AccountPlugin.Builder.anAccountPlugin;
import static software.wings.beans.PluginCategory.Artifact;
import static software.wings.beans.PluginCategory.CloudProvider;
import static software.wings.beans.PluginCategory.Collaboration;
import static software.wings.beans.PluginCategory.ConnectionAttributes;
import static software.wings.beans.PluginCategory.LoadBalancer;
import static software.wings.beans.PluginCategory.Verification;

import org.junit.Test;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AwsConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.ElasticLoadBalancerConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.SlackConfig;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.impl.PluginServiceImpl;
import software.wings.service.intfc.PluginService;
import software.wings.sm.StateType;

/**
 * Created by peeyushaggarwal on 10/21/16.
 */
public class PluginServiceTest {
  private PluginService pluginService = new PluginServiceImpl();

  @Test
  public void shouldGetInstalledPlugins() throws Exception {
    String accountId = "ACCOUNT_ID";

    assertThat(pluginService.getInstalledPlugins(accountId))
        .hasSize(18)
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
                .withSettingClass(DockerConfig.class)
                .withAccountId(accountId)
                .withIsEnabled(true)
                .withDisplayName("Docker Registry")
                .withType("DOCKER")
                .withPluginCategories(asList(Artifact))
                .build(),
            anAccountPlugin()
                .withSettingClass(NexusConfig.class)
                .withAccountId(accountId)
                .withIsEnabled(true)
                .withDisplayName("Nexus")
                .withType("NEXUS")
                .withPluginCategories(asList(Artifact))
                .build(),
            anAccountPlugin()
                .withSettingClass(ArtifactoryConfig.class)
                .withAccountId(accountId)
                .withIsEnabled(true)
                .withDisplayName("Artifactory")
                .withType("ARTIFACTORY")
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
                .withSettingClass(NewRelicConfig.class)
                .withAccountId(accountId)
                .withIsEnabled(true)
                .withDisplayName("NewRelic")
                .withType(StateType.NEW_RELIC.name())
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
                .withSettingClass(ElkConfig.class)
                .withAccountId(accountId)
                .withIsEnabled(true)
                .withDisplayName("ELK")
                .withType("ELK")
                .withPluginCategories(asList(Verification))
                .build(),
            anAccountPlugin()
                .withSettingClass(LogzConfig.class)
                .withAccountId(accountId)
                .withIsEnabled(true)
                .withDisplayName("LOGZ")
                .withType("LOGZ")
                .withPluginCategories(asList(Verification))
                .build(),
            anAccountPlugin()
                .withSettingClass(SumoConfig.class)
                .withAccountId(accountId)
                .withIsEnabled(true)
                .withDisplayName("SumoLogic")
                .withType("SUMO")
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
                .withDisplayName("Amazon Web Services")
                .withType("AWS")
                .withPluginCategories(asList(CloudProvider))
                .build(),
            anAccountPlugin()
                .withSettingClass(GcpConfig.class)
                .withAccountId(accountId)
                .withIsEnabled(true)
                .withDisplayName("Google Cloud Platform")
                .withType("GCP")
                .withPluginCategories(asList(CloudProvider))
                .build(),
            anAccountPlugin()
                .withSettingClass(PhysicalDataCenterConfig.class)
                .withAccountId(accountId)
                .withIsEnabled(true)
                .withDisplayName("Physical Data Center")
                .withType("PHYSICAL_DATA_CENTER")
                .withPluginCategories(asList(CloudProvider))
                .build(),
            anAccountPlugin()
                .withSettingClass(HostConnectionAttributes.class)
                .withAccountId(accountId)
                .withIsEnabled(false)
                .withDisplayName("Host Connection Attributes")
                .withType("HOST_CONNECTION_ATTRIBUTES")
                .withPluginCategories(asList(ConnectionAttributes))
                .build(),
            anAccountPlugin()
                .withSettingClass(ElasticLoadBalancerConfig.class)
                .withAccountId(accountId)
                .withIsEnabled(true)
                .withDisplayName("Elastic Classic Load Balancer")
                .withType("ELB")
                .withPluginCategories(asList(LoadBalancer))
                .build());
  }

  @Test
  public void shouldGetPluginSettingSchema() throws Exception {
    String accountId = "ACCOUNT_ID";

    assertThat(pluginService.getPluginSettingSchema(accountId))
        .hasSize(18)
        .containsOnlyKeys("APP_DYNAMICS", "NEW_RELIC", "JENKINS", "BAMBOO", "SMTP", "SLACK", "SPLUNK", "ELK", "LOGZ",
            "SUMO", "AWS", "GCP", "PHYSICAL_DATA_CENTER", "DOCKER", "HOST_CONNECTION_ATTRIBUTES", "ELB", "NEXUS",
            "ARTIFACTORY");
  }
}
