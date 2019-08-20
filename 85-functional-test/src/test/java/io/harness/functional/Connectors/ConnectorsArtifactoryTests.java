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
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.matchers.BooleanMatcher;
import io.harness.testframework.restutils.SettingsUtils;
import io.restassured.path.json.JsonPath;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectorsArtifactoryTests extends AbstractFunctionalTest {
  // Test Constants
  private static String CONNECTOR_NAME_NEXUS = "Automation-Nexus-Connector-" + System.currentTimeMillis();
  private static String CATEGORY = "CONNECTOR";
  private static String CONNECTOR_NAME_JENKINS = "Automation-Jenkins-Connector-" + System.currentTimeMillis();
  private static String CONNECTOR_NAME_DOCKER = "Automation-Docker-Connector-" + System.currentTimeMillis();
  private static String CONNECTOR_NAME_BAMBOO = "Automation-Bamboo-Connector-" + System.currentTimeMillis();
  private static String CONNECTOR_NAME_ARTIFACTORY = "Automation-Artifactory-Connector-" + System.currentTimeMillis();
  private static final Retry retry = new Retry(10, 1000);
  private static final BooleanMatcher booleanMatcher = new BooleanMatcher();
  // Test Entities

  private static String NexusConnectorId;
  private static String JenkinsConnectorId;
  private static String DockerConnectorId;
  private static String BambooConnectorId;
  private static String ArtifactoryConnectorId;

  @Test
  @Owner(emails = SUNIL, resent = false)
  @Category(FunctionalTests.class)
  public void TC0_listAllConnectors() {
    JsonPath connectors = SettingsUtils.listCloudproviderConnector(bearerToken, getAccount().getUuid(), CATEGORY);
    assertThat(connectors).isNotNull();
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void runNexusConnectorCRUDTests() {
    retry.executeWithRetry(this ::TC1_createNexusConnector, booleanMatcher, true);
    logger.info(String.format("Created  Nexus Connector with id %s", NexusConnectorId));
    TC2_updateNexusConnector();
    logger.info(String.format("Updated  Nexus Connector with id %s", NexusConnectorId));
    TC3_deleteNexusConnector();
    logger.info(String.format("Deleted  Nexus Connector with id %s", NexusConnectorId));
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void runJenkinsConnectorCRUDTests() {
    retry.executeWithRetry(this ::TC4_createJenkinsConnector, booleanMatcher, true);
    logger.info(String.format("Created Jenkins Connector with id %s", JenkinsConnectorId));
    TC5_updateJenkinsConnector();
    logger.info(String.format("Updated  Jenkins Connector with id %s", JenkinsConnectorId));
    TC6_deleteJenkinsConnector();
    logger.info(String.format("Deleted Jenkins Connector with id %s", JenkinsConnectorId));
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void runDockerConnectorCRUDTests() {
    retry.executeWithRetry(this ::TC7_createDockerConnector, booleanMatcher, true);
    logger.info(String.format("Created Docker Connector with id %s", DockerConnectorId));
    TC8_updateDockerConnector();
    logger.info(String.format("Updated Docker Connector with id %s", DockerConnectorId));
    TC9_deleteDockerConnector();
    logger.info(String.format("Deleted  Docker Connector with id %s", DockerConnectorId));
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void runBambooConnectorCRUDTests() {
    retry.executeWithRetry(this ::TC10_createBambooConnector, booleanMatcher, true);
    logger.info(String.format("Created  Bamboo Connector with id %s", BambooConnectorId));
    TC11_updateBambooConnector();
    logger.info(String.format("Updated  Bamboo Connector with id %s", BambooConnectorId));
    TC12_deleteBambooConnector();
    logger.info(String.format("Deleted  Bamboo Connector with id %s", BambooConnectorId));
  }

  @Test
  @Owner(emails = MEENAKSHI, resent = false)
  @Category(FunctionalTests.class)
  public void runArtifactoryConnectorCRUDTests() {
    retry.executeWithRetry(this ::TC13_createArtifactoryConnector, booleanMatcher, true);
    logger.info(String.format("Created Artifactory Connector with id %s", ArtifactoryConnectorId));
    // TC14_updateArtifactoryConnector();
    // logger.info(String.format("Updated  Artifactory Connector with id %s", ArtifactoryConnectorId));
    TC15_deleteArtifactoryConnector();
    logger.info(String.format("Deleted Artifactory Connector with id %s", ArtifactoryConnectorId));
  }

  public boolean TC1_createNexusConnector() {
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
    NexusConnectorId = setAttrResponse.getString("resource.uuid").trim();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_NEXUS);
    return connectorFound;
  }

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
        SettingsUtils.updateConnector(bearerToken, getAccount().getUuid(), NexusConnectorId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_NEXUS);
    assertTrue(connectorFound);
  }

  public void TC3_deleteNexusConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), NexusConnectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_NEXUS);
    assertFalse(connectorFound);
  }

  public boolean TC4_createJenkinsConnector() {
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

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, getAccount().getUuid(), settingAttribute);
    // asserting the response
    assertThat(setAttrResponse).isNotNull();
    JenkinsConnectorId = setAttrResponse.getString("resource.uuid").trim();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_JENKINS);
    return connectorFound;
  }

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
        SettingsUtils.updateConnector(bearerToken, getAccount().getUuid(), JenkinsConnectorId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_JENKINS);
    assertTrue(connectorFound);
  }

  public void TC6_deleteJenkinsConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), JenkinsConnectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_JENKINS);
    assertFalse(connectorFound);
  }

  public boolean TC7_createDockerConnector() {
    String DOCKER_URL = "https://registry.hub.docker.com/v2/";
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

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, getAccount().getUuid(), settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    DockerConnectorId = setAttrResponse.getString("resource.uuid").trim();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_DOCKER);
    return connectorFound;
  }

  public void TC8_updateDockerConnector() {
    CONNECTOR_NAME_DOCKER = CONNECTOR_NAME_DOCKER + "update";
    String DOCKER_URL = "https://registry.hub.docker.com/v2/";
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
        SettingsUtils.updateConnector(bearerToken, getAccount().getUuid(), DockerConnectorId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_DOCKER);
    assertTrue(connectorFound);
  }

  public void TC9_deleteDockerConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), DockerConnectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_DOCKER);
    assertFalse(connectorFound);
  }

  public boolean TC10_createBambooConnector() {
    String BAMBOO_URL = "http://ec2-18-208-86-222.compute-1.amazonaws.com:8085/";
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

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, getAccount().getUuid(), settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    BambooConnectorId = setAttrResponse.getString("resource.uuid").trim();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_BAMBOO);
    return connectorFound;
  }

  public void TC11_updateBambooConnector() {
    CONNECTOR_NAME_BAMBOO = CONNECTOR_NAME_BAMBOO + "update";
    String BAMBOO_URL = "http://ec2-18-208-86-222.compute-1.amazonaws.com:8085/";
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
        SettingsUtils.updateConnector(bearerToken, getAccount().getUuid(), BambooConnectorId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_BAMBOO);
    assertTrue(connectorFound);
  }

  public void TC12_deleteBambooConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), BambooConnectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_BAMBOO);
    assertFalse(connectorFound);
  }

  public boolean TC13_createArtifactoryConnector() {
    String ARTIFACTORY_URL = "https://harness.jfrog.io/harness";
    String USER_NAME = "admin";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(CONNECTOR_NAME_ARTIFACTORY)
            .withAccountId(getAccount().getUuid())
            .withValue(ArtifactoryConfig.builder()
                           .artifactoryUrl(ARTIFACTORY_URL)
                           .username(USER_NAME)
                           .password(new ScmSecret().decryptToCharArray(new SecretName("harness_artifactory")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, getAccount().getUuid(), settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    ArtifactoryConnectorId = setAttrResponse.getString("resource.uuid").trim();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_ARTIFACTORY);
    return connectorFound;
  }

  public void TC14_updateArtifactoryConnector() {
    CONNECTOR_NAME_ARTIFACTORY = CONNECTOR_NAME_ARTIFACTORY + "update";
    String ARTIFACTORY_URL = "https://harness.jfrog.io/harness";
    String USER_NAME = "admin";

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
        SettingsUtils.updateConnector(bearerToken, getAccount().getUuid(), ArtifactoryConnectorId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_ARTIFACTORY);
    assertTrue(connectorFound);
  }

  public void TC15_deleteArtifactoryConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), ArtifactoryConnectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_ARTIFACTORY);
    assertFalse(connectorFound);
  }
}