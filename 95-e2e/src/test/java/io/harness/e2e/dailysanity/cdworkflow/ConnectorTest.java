package io.harness.e2e.dailysanity.cdworkflow;

import static io.harness.rule.OwnerRule.NATARAJA;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.restutils.SettingsUtils;
import io.restassured.path.json.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.config.NexusConfig;

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectorTest extends AbstractE2ETest {
  // Test Constants
  private static String CONNECTOR_NAME = "Automation-Nexus-Connector-" + System.currentTimeMillis();
  private static String CATEGORY = "CONNECTOR";

  // Test Entities
  private static String connectorId;

  @Test
  @Owner(developers = NATARAJA)
  @Category(E2ETests.class)
  public void TC0_listAllConnectors() {
    JsonPath connectors = SettingsUtils.listCloudproviderConnector(bearerToken, getAccount().getUuid(), CATEGORY);
    assertThat(connectors).isNotNull();
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(E2ETests.class)
  public void TC1_createNexusConnector() {
    String NEXUS_URL = "https://nexus2.harness.io";
    String VERSION = "2.x";
    String USER_NAME = "admin";

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(SettingCategory.CONNECTOR)
            .withName(CONNECTOR_NAME)
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
    boolean connectorFound =
        SettingsUtils.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME);
    assertThat(connectorFound).isTrue();
  }

  @Test
  @Owner(developers = NATARAJA)
  @Category(E2ETests.class)
  public void TC2_deleteConnector() {
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), connectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound =
        SettingsUtils.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME);
    assertThat(connectorFound).isFalse();
  }
}