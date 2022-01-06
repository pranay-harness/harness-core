/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.restutils;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingAttribute.SettingCategory.CONNECTOR;
import static software.wings.utils.UsageRestrictionsUtils.getAllAppAllEnvUsageRestrictions;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.shell.AuthenticationScheme;

import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.BugsnagConfig;
import software.wings.beans.DatadogConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.security.UsageRestrictions;

import io.restassured.path.json.JsonPath;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConnectorUtils {
  public static String USER_NAME = "admin";

  public static String createArtifactoryConnector(String bearerToken, String connectorName, String accountId) {
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withName(connectorName)
            .withCategory(CONNECTOR)
            .withAccountId(accountId)
            .withValue(
                ArtifactoryConfig.builder()
                    .accountId(accountId)
                    .artifactoryUrl("https://harness.jfrog.io/harness")
                    .username("admin")
                    .password(new ScmSecret().decryptToCharArray(new SecretName("artifactory_connector_password")))
                    .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    assertThat(setAttrResponse.getString("resource.uuid")).withFailMessage(setAttrResponse.prettify()).isNotNull();
    return setAttrResponse.getString("resource.uuid").trim();
  }

  public static String createNexus2Connector(String bearerToken, String connectorName, String accountId) {
    String NEXUS_URL = "https://nexus2.harness.io";
    String VERSION = "2.x";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(connectorName)
            .withAccountId(accountId)
            .withValue(NexusConfig.builder()
                           .nexusUrl(NEXUS_URL)
                           .version(VERSION)
                           .username(USER_NAME)
                           .password(new ScmSecret().decryptToCharArray(new SecretName("harness_nexus")))
                           .accountId(accountId)
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    return setAttrResponse.getString("resource.uuid").trim();
  }

  public static String createNexus3Connector(String bearerToken, String connectorName, String accountId) {
    String NEXUS_URL = "https://nexus3.harness.io";
    String VERSION = "3.x";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(connectorName)
            .withAccountId(accountId)
            .withValue(NexusConfig.builder()
                           .nexusUrl(NEXUS_URL)
                           .version(VERSION)
                           .username(USER_NAME)
                           .password(new ScmSecret().decryptToCharArray(new SecretName("harness_nexus")))
                           .accountId(accountId)
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    return setAttrResponse.getString("resource.uuid").trim();
  }

  public static String createDockerRegistryConnector(String bearerToken, String connectorName, String accountId) {
    String DOCKER_URL = "https://registry.hub.docker.com/v2/";
    String USER_NAME = "wingsplugins";
    UsageRestrictions defaultUsageRestrictions = getAllAppAllEnvUsageRestrictions();

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(connectorName)
            .withAccountId(accountId)
            .withUsageRestrictions(defaultUsageRestrictions)
            .withValue(DockerConfig.builder()
                           .dockerRegistryUrl(DOCKER_URL)
                           .username(USER_NAME)
                           .password(new ScmSecret().decryptToCharArray(new SecretName("harness_docker")))
                           .accountId(accountId)
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    return setAttrResponse.getString("resource.uuid").trim();
  }

  /**
   * Connector util to create a Verification Provider: AppDynamics Connector
   */
  public static String createAppDynamicsConnector(String bearerToken, String connectorName, String accountId) {
    String APPDYNAMICS_URL = "https://harness-test.saas.appdynamics.com/controller";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(connectorName)
            .withAccountId(accountId)
            .withValue(AppDynamicsConfig.builder()
                           .controllerUrl(APPDYNAMICS_URL)
                           .accountname(String.valueOf(
                               new ScmSecret().decryptToCharArray(new SecretName("appd_config_accountname"))))
                           .username(String.valueOf(
                               new ScmSecret().decryptToCharArray(new SecretName("appd_config_username"))))
                           .password(new ScmSecret().decryptToCharArray(new SecretName("appd_config_password_test")))
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    return setAttrResponse.getString("resource.uuid").trim();
  }

  /**
   * Connector util to create a Verification Provider: NewRelic Connector
   */
  public static String createNewrelicConnector(String bearerToken, String connectorName, String accountId) {
    String NEW_RELIC_URL = "https://api.newrelic.com";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(connectorName)
            .withAccountId(accountId)
            .withValue(NewRelicConfig.builder()
                           .newRelicUrl(NEW_RELIC_URL)
                           .apiKey(new ScmSecret().decryptToCharArray(new SecretName("new_relic_api_key")))
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    return setAttrResponse.getString("resource.uuid").trim();
  }

  /**
   * Connector util to create a Verification Provider: ELK Connector
   */
  public static String createELKConnector(String bearerToken, String connectorName, String accountId) {
    String ELK_URL = "http://ec2-34-227-84-170.compute-1.amazonaws.com:9200/";

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withCategory(SettingCategory.CONNECTOR)
                                            .withName(connectorName)
                                            .withAccountId(accountId)
                                            .withValue(ElkConfig.builder().elkUrl(ELK_URL).build())
                                            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    return setAttrResponse.getString("resource.uuid").trim();
  }

  /**
   * Connector util to create a Verification Provider: Splunk Connector
   */
  public static String createSplunkConnector(String bearerToken, String connectorName, String accountId) {
    String SPLUNK_URL = "https://api-prd-p-429h4vj2lsng.cloud.splunk.com:8089/";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(connectorName)
            .withAccountId(accountId)
            .withValue(SplunkConfig.builder()
                           .splunkUrl(SPLUNK_URL)
                           .username(String.valueOf(
                               new ScmSecret().decryptToCharArray(new SecretName("splunk_cloud_username"))))
                           .password(new ScmSecret().decryptToCharArray(new SecretName("splunk_cloud_password")))
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    return setAttrResponse.getString("resource.uuid").trim();
  }

  /**
   * Connector util to create a Verification Provider: Bugsnag Connector
   */
  public static String createBugsnagConnector(String bearerToken, String connectorName, String accountId) {
    String BUGSNAG_URL = "https://api.bugsnag.com/";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(connectorName)
            .withAccountId(accountId)
            .withValue(BugsnagConfig.builder()
                           .url(BUGSNAG_URL)
                           .authToken(new ScmSecret().decryptToCharArray(new SecretName("bugsnag_config_auth_token")))
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    return setAttrResponse.getString("resource.uuid").trim();
  }

  /**
   * Connector util to create a Verification Provider: Datadog Connector
   */
  public static String createDatadogConnector(String bearerToken, String connectorName, String accountId) {
    String DATADOG_URL = "https://app.datadoghq.com/api/v1/";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(connectorName)
            .withAccountId(accountId)
            .withValue(
                DatadogConfig.builder()
                    .url(DATADOG_URL)
                    .apiKey(new ScmSecret().decryptToCharArray(new SecretName("datadog_api_key")))
                    .applicationKey(new ScmSecret().decryptToCharArray(new SecretName("datadog_application_key")))
                    .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    return setAttrResponse.getString("resource.uuid").trim();
  }

  /**
   * Connector util to create a Verification Provider: Sumo Logic Connector
   */
  public static String createSumoLogicConnector(String bearerToken, String connectorName, String accountId) {
    String SUMOLOGIC_URL = "https://api.us2.sumologic.com/api/v1/";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(connectorName)
            .withAccountId(accountId)
            .withValue(SumoConfig.builder()
                           .sumoUrl(SUMOLOGIC_URL)
                           .accessId(new ScmSecret().decryptToCharArray(new SecretName("sumo_config_access_id")))
                           .accessKey(new ScmSecret().decryptToCharArray(new SecretName("sumo_config_access_key")))
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    return setAttrResponse.getString("resource.uuid").trim();
  }

  /**
   * Connector util to create a Verification Provider: Prometheus Connector
   */
  public static String createPrometheusConnector(String bearerToken, String connectorName, String accountId) {
    String PROMETHEUS_URL = "http://35.188.150.89:8080/";

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withCategory(SettingCategory.CONNECTOR)
                                            .withName(connectorName)
                                            .withAccountId(accountId)
                                            .withValue(PrometheusConfig.builder().url(PROMETHEUS_URL).build())
                                            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    return setAttrResponse.getString("resource.uuid").trim();
  }
  /**
   * Connector util to create a Source Repo Provider: GitHub Connector
   * Returns a List Consisting: UUID and Webhook URL
   */
  public static List<String> createGitConnector(String bearerToken, String connectorName, String accountId) {
    String REPO_URL = "https://github.com/wings-software/automation-testing";
    String BRANCH = "master";
    List<String> details = new ArrayList<>();

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(connectorName)
            .withAccountId(accountId)
            .withValue(GitConfig.builder()
                           .repoUrl(REPO_URL)
                           .generateWebhookUrl(true)
                           .branch(BRANCH)
                           .authenticationScheme(AuthenticationScheme.HTTP_PASSWORD)
                           .username(String.valueOf(
                               new ScmSecret().decryptToCharArray(new SecretName("git_automation_username"))))
                           .password(new ScmSecret().decryptToCharArray(new SecretName("git_automation_password")))
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, accountId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    String uuid = setAttrResponse.getString("resource.uuid").trim();
    String webhookToken = setAttrResponse.getString("resource.value.webhookToken").trim();

    details.add(uuid);
    details.add(webhookToken);

    return details;
  }
}
