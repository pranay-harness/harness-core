package io.harness.functional.Connectors;

import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.MEENAKSHI;
import static io.harness.rule.OwnerRule.NATARAJA;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.Retry;
import io.harness.testframework.framework.matchers.BooleanMatcher;
import io.harness.testframework.restutils.SettingsUtils;
import io.restassured.path.json.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
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
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectorsArtifactoryTests extends AbstractFunctionalTest {
  // Test Constants
  private static String CONNECTOR_NAME_NEXUS = "Automation-Nexus-Connector-" + System.currentTimeMillis();
  private static String CATEGORY = "CONNECTOR";
  private static String AZURE_ARTIFACTS_CATEGORY = "AZURE_ARTIFACTS";
  private static String CONNECTOR_NAME_JENKINS = "Automation-Jenkins-Connector-" + System.currentTimeMillis();
  private static String CONNECTOR_NAME_DOCKER = "Automation-Docker-Connector-" + System.currentTimeMillis();
  private static String CONNECTOR_NAME_BAMBOO = "Automation-Bamboo-Connector-" + System.currentTimeMillis();
  private static String CONNECTOR_NAME_ARTIFACTORY = "Automation-Artifactory-Connector-" + System.currentTimeMillis();
  private static String CONNECTOR_NAME_AZURE_ARTIFACTS =
      "Automation-AzureArtifacts-Connector-" + System.currentTimeMillis();
  private static final Retry retry = new Retry(10, 1000);
  private static final BooleanMatcher booleanMatcher = new BooleanMatcher();
  // Test Entities

  private static String NexusConnectorId;
  private static String JenkinsConnectorId;
  private static String DockerConnectorId;
  private static String BambooConnectorId;
  private static String ArtifactoryConnectorId;
  private static String AzureArtifactsConnectorId;

  @Test
  @Owner(developers = NATARAJA)
  @Category(FunctionalTests.class)
  public void TC0_listAllConnectors() {
    JsonPath connectors = SettingsUtils.listCloudproviderConnector(bearerToken, getAccount().getUuid(), CATEGORY);
    assertThat(connectors).isNotNull();
  }

  @Test
  @Owner(developers = MEENAKSHI, intermittent = true)
  @Category(FunctionalTests.class)
  public void runNexusConnectorCRUDTests() {
    retry.executeWithRetry(this ::TC1_createNexusConnector, booleanMatcher, true);
    logger.info("Created  Nexus Connector with id {}", NexusConnectorId);
    TC2_updateNexusConnector();
    logger.info("Updated  Nexus Connector with id {}", NexusConnectorId);
    TC3_deleteNexusConnector();
    logger.info("Deleted  Nexus Connector with id {}", NexusConnectorId);
  }

  @Test
  @Owner(developers = MEENAKSHI, intermittent = true)
  @Category(FunctionalTests.class)
  public void runJenkinsConnectorCRUDTests() {
    retry.executeWithRetry(this ::TC4_createJenkinsConnector, booleanMatcher, true);
    logger.info("Created Jenkins Connector with id {}", JenkinsConnectorId);
    TC5_updateJenkinsConnector();
    logger.info("Updated  Jenkins Connector with id {}", JenkinsConnectorId);
    TC6_deleteJenkinsConnector();
    logger.info("Deleted Jenkins Connector with id {}", JenkinsConnectorId);
  }

  @Test
  @Owner(developers = MEENAKSHI, intermittent = true)
  @Category(FunctionalTests.class)
  public void runDockerConnectorCRUDTests() {
    retry.executeWithRetry(this ::TC7_createDockerConnector, booleanMatcher, true);
    logger.info("Created Docker Connector with id {}", DockerConnectorId);
    TC8_updateDockerConnector();
    logger.info("Updated Docker Connector with id {}", DockerConnectorId);
    TC9_deleteDockerConnector();
    logger.info("Deleted  Docker Connector with id {}", DockerConnectorId);
  }

  @Test
  @Owner(developers = MEENAKSHI, intermittent = true)
  @Category(FunctionalTests.class)
  public void runBambooConnectorCRUDTests() {
    retry.executeWithRetry(this ::TC10_createBambooConnector, booleanMatcher, true);
    logger.info("Created  Bamboo Connector with id {}", BambooConnectorId);
    TC11_updateBambooConnector();
    logger.info("Updated  Bamboo Connector with id {}", BambooConnectorId);
    TC12_deleteBambooConnector();
    logger.info("Deleted  Bamboo Connector with id {}", BambooConnectorId);
  }

  @Test
  @Owner(developers = MEENAKSHI, intermittent = true)
  @Category(FunctionalTests.class)
  @Ignore("Enable it once jfrog admin credentials are available ")
  public void runArtifactoryConnectorCRUDTests() {
    retry.executeWithRetry(this ::TC13_createArtifactoryConnector, booleanMatcher, true);
    logger.info("Created Artifactory Connector with id {}", ArtifactoryConnectorId);
    // TC14_updateArtifactoryConnector();
    // logger.info("Updated  Artifactory Connector with id {}", ArtifactoryConnectorId);
    TC15_deleteArtifactoryConnector();
    logger.info("Deleted Artifactory Connector with id {}", ArtifactoryConnectorId);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(FunctionalTests.class)
  @Ignore("TODO: Fix azure artifacts tests - repo config seems to have changed")
  public void runAzureArtifactsConnectorCRUDTests() {
    retry.executeWithRetry(this ::TC16_createAzureArtifactsConnector, booleanMatcher, true);
    logger.info(String.format("Created Azure Artifacts Connector with id %s", AzureArtifactsConnectorId));
    TC17_updateAzureArtifactsConnector();
    logger.info(String.format("Updated Azure Artifacts Connector with id %s", AzureArtifactsConnectorId));
    TC18_deleteAzureArtifactsConnector();
    logger.info(String.format("Deleted Azure Artifacts Connector with id %s", AzureArtifactsConnectorId));
  }

  public boolean TC1_createNexusConnector() {
    String NEXUS_URL = "https://nexus2.harness.io";
    String VERSION = "2.x";
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
    String VERSION = "2.x";
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
    assertThat(connectorFound).isTrue();
  }

  public void TC3_deleteNexusConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), NexusConnectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_NEXUS);
    assertThat(connectorFound).isFalse();
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
    assertThat(connectorFound).isTrue();
  }

  public void TC6_deleteJenkinsConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), JenkinsConnectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_JENKINS);
    assertThat(connectorFound).isFalse();
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
    assertThat(connectorFound).isTrue();
  }

  public void TC9_deleteDockerConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), DockerConnectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_DOCKER);
    assertThat(connectorFound).isFalse();
  }

  public boolean TC10_createBambooConnector() {
    String BAMBOO_URL = "http://cdteam-bamboo.harness.io:8085/";
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
    String BAMBOO_URL = "http://cdteam-bamboo.harness.io:8085/";
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
    assertThat(connectorFound).isTrue();
  }

  public void TC12_deleteBambooConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), BambooConnectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_BAMBOO);
    assertThat(connectorFound).isFalse();
  }

  public boolean TC13_createArtifactoryConnector() {
    String ARTIFACTORY_URL = "https://jfrog.dev.harness.io/artifactory/";
    String USER_NAME = "harnessadmin";

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
    assertThat(connectorFound).isTrue();
  }

  public void TC15_deleteArtifactoryConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), ArtifactoryConnectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME_ARTIFACTORY);
    assertThat(connectorFound).isFalse();
  }

  private boolean TC16_createAzureArtifactsConnector() {
    String azureDevopsUrl = "https://dev.azure.com/garvit-test";
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.AZURE_ARTIFACTS)
            .withName(CONNECTOR_NAME_AZURE_ARTIFACTS)
            .withAccountId(getAccount().getUuid())
            .withValue(AzureArtifactsPATConfig.builder()
                           .azureDevopsUrl(azureDevopsUrl)
                           .pat(new ScmSecret().decryptToCharArray(new SecretName("harness_azure_devops_pat")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse = SettingsUtils.create(bearerToken, getAccount().getUuid(), settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    AzureArtifactsConnectorId = setAttrResponse.getString("resource.uuid").trim();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), AZURE_ARTIFACTS_CATEGORY, CONNECTOR_NAME_AZURE_ARTIFACTS);
    return connectorFound;
  }

  private void TC17_updateAzureArtifactsConnector() {
    CONNECTOR_NAME_AZURE_ARTIFACTS = CONNECTOR_NAME_AZURE_ARTIFACTS + "update";
    String azureDevopsUrl = "https://dev.azure.com/garvit-test";
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.AZURE_ARTIFACTS)
            .withName(CONNECTOR_NAME_AZURE_ARTIFACTS)
            .withAccountId(getAccount().getUuid())
            .withValue(AzureArtifactsPATConfig.builder()
                           .azureDevopsUrl(azureDevopsUrl)
                           .pat(new ScmSecret().decryptToCharArray(new SecretName("harness_azure_devops_pat")))
                           .accountId(getAccount().getUuid())
                           .build())
            .build();

    JsonPath setAttrResponse =
        SettingsUtils.updateConnector(bearerToken, getAccount().getUuid(), AzureArtifactsConnectorId, settingAttribute);
    assertThat(setAttrResponse).isNotNull();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), AZURE_ARTIFACTS_CATEGORY, CONNECTOR_NAME_AZURE_ARTIFACTS);
    assertThat(connectorFound).isTrue();
  }

  private void TC18_deleteAzureArtifactsConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), AzureArtifactsConnectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound = SettingsUtils.checkCloudproviderConnectorExist(
        bearerToken, getAccount().getUuid(), AZURE_ARTIFACTS_CATEGORY, CONNECTOR_NAME_AZURE_ARTIFACTS);
    assertThat(connectorFound).isFalse();
  }
}
