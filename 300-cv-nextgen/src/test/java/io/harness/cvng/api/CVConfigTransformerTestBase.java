package io.harness.cvng.api;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.CvNextGenTest;
import io.harness.cvng.core.beans.DSConfig;
import io.harness.cvng.core.entities.CVConfig;

import org.junit.Before;

public class CVConfigTransformerTestBase extends CvNextGenTest {
  protected String identifier;
  protected String accountId;
  protected String projectIdentifier;
  protected String productName;
  protected String connectorIdentifier;
  protected String envIdentifier;
  protected String groupId;
  protected String serviceIdentifier;
  protected String monitoringSourceName;

  @Before
  public void setUp() {
    identifier = generateUuid();
    groupId = identifier;
    monitoringSourceName = generateUuid();
    accountId = generateUuid();
    projectIdentifier = "harness";
    productName = "Performance monitoring";
    envIdentifier = "production_harness";
    serviceIdentifier = "manager";
  }

  protected void fillCommonFields(DSConfig dsConfig) {
    dsConfig.setAccountId(accountId);
    dsConfig.setConnectorIdentifier(connectorIdentifier);
    dsConfig.setIdentifier(identifier);
    dsConfig.setMonitoringSourceName(monitoringSourceName);
    dsConfig.setProductName(productName);
    dsConfig.setProjectIdentifier(projectIdentifier);
  }

  protected void fillCommonFields(CVConfig cvConfig) {
    cvConfig.setEnvIdentifier(envIdentifier);
    cvConfig.setConnectorIdentifier(connectorIdentifier);
    cvConfig.setProductName(productName);
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setAccountId(accountId);
    cvConfig.setIdentifier(groupId);
    cvConfig.setMonitoringSourceName(monitoringSourceName);
    cvConfig.setServiceIdentifier(serviceIdentifier);
  }
}
