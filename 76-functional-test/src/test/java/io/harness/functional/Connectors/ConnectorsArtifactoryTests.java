package io.harness.functional.Connectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import io.harness.RestUtils.SettingsUtil;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.restassured.path.json.JsonPath;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.config.NexusConfig;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectorsArtifactoryTests extends AbstractFunctionalTest {
  // Test Constants
  private static String CONNECTOR_NAME = "Automation-Nexus-Connector-" + System.currentTimeMillis();
  private static String CATEGORY = "CONNECTOR";

  // Test Entities
  private static String connectorId;

  @Test
  @Owner(emails = "sunil@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void TC0_listAllConnectors() {
    JsonPath connectors = SettingsUtil.listCloudproviderConnector(bearerToken, getAccount().getUuid(), CATEGORY);
    assertThat(connectors).isNotNull();
  }

  @Test
  @Owner(emails = "sunil@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void TC1_createNexusConnector() {
    String NEXUS_URL = "https://nexus2.harness.io";
    String VERSION = "3.x";
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

    JsonPath setAttrResponse = SettingsUtil.create(bearerToken, getAccount().getUuid(), settingAttribute);
    assertThat(setAttrResponse).isNotNull();
    connectorId = setAttrResponse.getString("resource.uuid").trim();

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound =
        SettingsUtil.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME);
    assertTrue(connectorFound);
  }

  @Test
  @Owner(emails = "sunil@harness.io", resent = false)
  @Category(FunctionalTests.class)
  public void TC2_deleteConnector() {
    SettingsUtil.delete(bearerToken, getAccount().getUuid(), connectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    boolean connectorFound =
        SettingsUtil.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, CONNECTOR_NAME);
    assertFalse(connectorFound);
  }
}