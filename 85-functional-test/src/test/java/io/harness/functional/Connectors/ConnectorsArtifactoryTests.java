package io.harness.functional.Connectors;

import static io.harness.rule.OwnerRule.MEENAKSHI;
import static io.harness.rule.OwnerRule.SUNIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.restutils.SettingsUtils;
import io.restassured.path.json.JsonPath;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
// import software.wings.beans.settings.helm.HttpHelmRepoConfig;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectorsArtifactoryTests extends AbstractFunctionalTest {
  // Test Constants
  private static String CONNECTOR_NAME_NEXUS = "Automation-Nexus-Connector-" + System.currentTimeMillis();
  private static String CATEGORY = "CONNECTOR";
  private static String CONNECTOR_NAME_JENKINS = "Automation-Jenkins-Connector-" + System.currentTimeMillis();
  private static String CONNECTOR_NAME_DOCKER = "Automation-Docker-Connector-" + System.currentTimeMillis();
  private static String CONNECTOR_NAME_BAMBOO = "Automation-Bamboo-Connector-" + System.currentTimeMillis();
  private static String CONNECTOR_NAME_ARTIFACTORY = "Automation-Artifactory-Connector-" + System.currentTimeMillis();
  // private static String CONNECTOR_NAME_HELM = "Automation-Helm-Connector-" + System.currentTimeMillis();
  // private static String CONNECTOR_NAME_SMB = "Automation-SMB-Connector-" + System.currentTimeMillis();
  // private static String CONNECTOR_NAME_SFTP = "Automation-SFTP-Connector-" + System.currentTimeMillis();
  // Test Entities
  private static String connectorId;

  @Test
  @Owner(emails = SUNIL, resent = false)
  @Category(FunctionalTests.class)
  public void TC0_listAllConnectors() {
    JsonPath connectors = SettingsUtils.listCloudproviderConnector(bearerToken, getAccount().getUuid(), CATEGORY);
    assertThat(connectors).isNotNull();
  }

  @Test
  @Owner(emails = SUNIL, resent = false)
  @Category(FunctionalTests.class)
  public void TC1_createNexusConnector() {
    String NEXUS_URL = "https://nexus2.harness.io";
    String VERSION = "3.x";
    String USER_NAME = "admin";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(CONNECTOR_NAME_NEXUS)
            .withAccountId(getAccount().getUuid())
            .withValue(NexusConfig.builder()
                           .nexusUrl(NEXUS_URL)
                           .version(VERSION)
                           .username(USER_NAME)
                           .password(new ScmSecret().decryptToCharArray(new SecretName("harness_nexus")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, getAccount().getUuid(), settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    connectorId = setAttrResponse.getString("resource.uuid").trim();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_NEXUS);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)

  public void TC2_updateNexusConnector() {
    String NEXUS_URL = "https://nexus2.harness.io";
    String VERSION = "3.x";
    String USER_NAME = "admin";
    CONNECTOR_NAME_NEXUS = CONNECTOR_NAME_NEXUS + "update";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(CONNECTOR_NAME_NEXUS)
            .withAccountId(getAccount().getUuid())
            .withValue(NexusConfig.builder()
                           .nexusUrl(NEXUS_URL)
                           .version(VERSION)
                           .username(USER_NAME)
                           .password(new ScmSecret().decryptToCharArray(new SecretName("harness_nexus")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse =
        SettingsUtils.updateConnector(bearerToken, getAccount().getUuid(), connectorId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_NEXUS);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = SUNIL, resent = false)
  @Category(FunctionalTests.class)
  public void TC3_deleteNexusConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), connectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_NEXUS);
    assertFalse(connectorFound);
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void TC4_createJenkinsConnector() {
    String JENKINS_URL = "https://jenkinsint.harness.io/";
    // String VERSION = "3.x";
    String USER_NAME = "wingsbuild";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(CONNECTOR_NAME_JENKINS)
            .withAccountId(getAccount().getUuid())
            .withValue(JenkinsConfig.builder()
                           .jenkinsUrl(JENKINS_URL)
                           // .version(VERSION)
                           .username(USER_NAME)
                           .password(new ScmSecret().decryptToCharArray(new SecretName("harness_jenkins")))
                           .authMechanism("Username/Password")
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, getAccount().getUuid(), settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    connectorId = setAttrResponse.getString("resource.uuid").trim();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_JENKINS);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void TC5_updateJenkinsConnector() {
    CONNECTOR_NAME_JENKINS = CONNECTOR_NAME_JENKINS + "update";
    String JENKINS_URL = "https://jenkinsint.harness.io/";

    String USER_NAME = "wingsbuild";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(CONNECTOR_NAME_JENKINS)
            .withAccountId(getAccount().getUuid())
            .withValue(JenkinsConfig.builder()
                           .jenkinsUrl(JENKINS_URL)
                           // .version(VERSION)
                           .username(USER_NAME)
                           .password(new ScmSecret().decryptToCharArray(new SecretName("harness_jenkins")))
                           .authMechanism("Username/Password")
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse =
        SettingsUtils.updateConnector(bearerToken, getAccount().getUuid(), connectorId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_JENKINS);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void TC6_deleteJenkinsConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), connectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_JENKINS);
    assertFalse(connectorFound);
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void TC7_createDockerConnector() {
    String DOCKER_URL = "https://registry.hub.docker.com/v2/";
    // String VERSION = "3.x";
    String USER_NAME = "";
    // char[] pass="06b13aea6f5f13ec69577689a899bbaad69eeb2f";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(CONNECTOR_NAME_DOCKER)
            .withAccountId(getAccount().getUuid())
            .withValue(DockerConfig.builder()
                           .dockerRegistryUrl(DOCKER_URL)
                           // .version(VERSION)
                           .username(USER_NAME)
                           .password(new ScmSecret().decryptToCharArray(new SecretName("harness_docker_v2")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, getAccount().getUuid(), settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    connectorId = setAttrResponse.getString("resource.uuid").trim();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_DOCKER);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void TC8_updateDockerConnector() {
    CONNECTOR_NAME_DOCKER = CONNECTOR_NAME_DOCKER + "update";

    String DOCKER_URL = "https://registry.hub.docker.com/v2/";
    // String VERSION = "3.x";
    String USER_NAME = "";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(CONNECTOR_NAME_DOCKER)
            .withAccountId(getAccount().getUuid())
            .withValue(DockerConfig.builder()
                           .dockerRegistryUrl(DOCKER_URL)
                           // .version(VERSION)
                           .username(USER_NAME)
                           .password(new ScmSecret().decryptToCharArray(new SecretName("harness_docker_v2")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse =
        SettingsUtils.updateConnector(bearerToken, getAccount().getUuid(), connectorId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_DOCKER);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void TC9_deleteDockerConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), connectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_DOCKER);
    assertFalse(connectorFound);
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void TC10_createBambooConnector() {
    String BAMBOO_URL = "http://ec2-18-208-86-222.compute-1.amazonaws.com:8085/";
    // String VERSION = "3.x";
    String USER_NAME = "wingsbuild";
    // char[] pass="06b13aea6f5f13ec69577689a899bbaad69eeb2f";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(CONNECTOR_NAME_BAMBOO)
            .withAccountId(getAccount().getUuid())
            .withValue(BambooConfig.builder()
                           .bambooUrl(BAMBOO_URL)
                           // .version(VERSION)
                           .username(USER_NAME)
                           .password(new ScmSecret().decryptToCharArray(new SecretName("harness_bamboo")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, getAccount().getUuid(), settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    connectorId = setAttrResponse.getString("resource.uuid").trim();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_BAMBOO);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void TC11_updateBambooConnector() {
    CONNECTOR_NAME_BAMBOO = CONNECTOR_NAME_BAMBOO + "update";

    String BAMBOO_URL = "http://ec2-18-208-86-222.compute-1.amazonaws.com:8085/";
    // String VERSION = "3.x";
    String USER_NAME = "wingsbuild";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(CONNECTOR_NAME_BAMBOO)
            .withAccountId(getAccount().getUuid())
            .withValue(BambooConfig.builder()
                           .bambooUrl(BAMBOO_URL)
                           // .version(VERSION)
                           .username(USER_NAME)
                           .password(new ScmSecret().decryptToCharArray(new SecretName("harness_bamboo")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse =
        SettingsUtils.updateConnector(bearerToken, getAccount().getUuid(), connectorId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_BAMBOO);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void TC12_deleteBambooConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), connectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_BAMBOO);
    assertFalse(connectorFound);
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void TC13_createArtifactoryConnector() {
    String ARTIFACTORY_URL = "https://harness.jfrog.io/harness";
    // String VERSION = "3.x";
    String USER_NAME = "admin";
    // char[] pass="06b13aea6f5f13ec69577689a899bbaad69eeb2f";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(CONNECTOR_NAME_ARTIFACTORY)
            .withAccountId(getAccount().getUuid())
            .withValue(ArtifactoryConfig.builder()
                           .artifactoryUrl(ARTIFACTORY_URL)
                           // .version(VERSION)
                           .username(USER_NAME)
                           .password(new ScmSecret().decryptToCharArray(new SecretName("harness_artifactory")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, getAccount().getUuid(), settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    connectorId = setAttrResponse.getString("resource.uuid").trim();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_ARTIFACTORY);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void TC14_updateArtifactoryConnector() {
    CONNECTOR_NAME_ARTIFACTORY = CONNECTOR_NAME_ARTIFACTORY + "update";

    String ARTIFACTORY_URL = "https://harness.jfrog.io/harness";
    // String VERSION = "3.x";
    String USER_NAME = "admin";
    // char[] pass="06b13aea6f5f13ec69577689a899bbaad69eeb2f";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(CONNECTOR_NAME_ARTIFACTORY)
            .withAccountId(getAccount().getUuid())
            .withValue(ArtifactoryConfig.builder()
                           .artifactoryUrl(ARTIFACTORY_URL)
                           // .version(VERSION)
                           .username(USER_NAME)
                           .password(new ScmSecret().decryptToCharArray(new SecretName("harness_artifactory")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse =
        SettingsUtils.updateConnector(bearerToken, getAccount().getUuid(), connectorId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_ARTIFACTORY);
    assertThat(connectorFound).isTrue();
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void TC15_deleteArtifactoryConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), connectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_ARTIFACTORY);
    assertFalse(connectorFound);
  }
}
