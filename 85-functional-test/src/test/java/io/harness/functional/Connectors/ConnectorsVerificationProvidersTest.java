package io.harness.functional.Connectors;

import static io.harness.rule.OwnerRule.SHASWAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.restutils.ConnectorUtils;
import io.harness.testframework.restutils.SettingsUtils;
import io.restassured.path.json.JsonPath;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectorsVerificationProvidersTest extends AbstractFunctionalTest {
  private static String CONNECTOR_APPDYNAMICS_PREFIX = "Automation-Appdynamics-Connector";
  private static String CONNECTOR_NEWRELIC_PREFIX = "Automation-Newrelic-Connector";
  private static String CONNECTOR_ELK_PREFIX = "Automation-ELK-Connector";
  private static String CONNECTOR_SPLUNK_PREFIX = "Automation-SPLUNK-Connector";
  private static String CATEGORY = "CONNECTOR";

  @Test
  @Owner(emails = SHASWAT, resent = false)
  @Category(FunctionalTests.class)
  public void TC0_listAllConnectors() {
    JsonPath connectors = SettingsUtils.listCloudproviderConnector(bearerToken, getAccount().getUuid(), CATEGORY);
    assertThat(connectors).isNotNull();
  }

  @Test
  @Owner(emails = SHASWAT, resent = false)
  @Category(FunctionalTests.class)
  public void TC1_createAndDeleteAppdynamicsConnector() {
    String connectorName = CONNECTOR_APPDYNAMICS_PREFIX + System.currentTimeMillis();
    String connectorId = ConnectorUtils.createAppDynamicsConnector(bearerToken, connectorName, getAccount().getUuid());

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound =
        SettingsUtils.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, connectorName);
    assertTrue(connectorFound);

    // Delete the created connector
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), connectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    connectorFound =
        SettingsUtils.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, connectorName);
    assertFalse(connectorFound);
  }

  @Test
  @Owner(emails = SHASWAT, resent = false)
  @Category(FunctionalTests.class)
  public void TC2_createAndDeleteNewrelicConnector() {
    String connectorName = CONNECTOR_NEWRELIC_PREFIX + System.currentTimeMillis();
    String connectorId = ConnectorUtils.createNewrelicConnector(bearerToken, connectorName, getAccount().getUuid());

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound =
        SettingsUtils.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, connectorName);
    assertTrue(connectorFound);

    // Delete the created connector
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), connectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    connectorFound =
        SettingsUtils.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, connectorName);
    assertFalse(connectorFound);
  }

  @Test
  @Owner(emails = SHASWAT, resent = false)
  @Category(FunctionalTests.class)
  public void TC3_createAndDeleteELKConnector() {
    String connectorName = CONNECTOR_ELK_PREFIX + System.currentTimeMillis();
    String connectorId = ConnectorUtils.createELKConnector(bearerToken, connectorName, getAccount().getUuid());

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound =
        SettingsUtils.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, connectorName);
    assertTrue(connectorFound);

    // Delete the created connector
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), connectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    connectorFound =
        SettingsUtils.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, connectorName);
    assertFalse(connectorFound);
  }

  @Test
  @Owner(emails = SHASWAT, resent = false)
  @Category(FunctionalTests.class)
  public void TC4_createAndDeleteSplunkConnector() {
    String connectorName = CONNECTOR_SPLUNK_PREFIX + System.currentTimeMillis();
    String connectorId = ConnectorUtils.createSplunkConnector(bearerToken, connectorName, getAccount().getUuid());

    // Verify connector is created i.e connector with specific name exist
    boolean connectorFound =
        SettingsUtils.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, connectorName);
    assertTrue(connectorFound);

    // Delete the created connector
    SettingsUtils.delete(bearerToken, getAccount().getUuid(), connectorId);

    // Verify connector is deleted i.e connector with specific name doesn't exist
    connectorFound =
        SettingsUtils.checkCloudproviderConnectorExist(bearerToken, getAccount().getUuid(), CATEGORY, connectorName);
    assertFalse(connectorFound);
  }
}
