/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.cvng.core.entities.CVConfig;

import org.junit.Before;

public class DSConfigTestBase extends CategoryTest {
  protected String monitoringSourceIdentifier;
  protected String monitoringSourceName;
  protected String accountId;
  protected String projectIdentifier;
  protected String productName;
  protected String connectorIdentifier;
  protected String envIdentifier;
  protected String serviceIdentifier;

  @Before
  public void setUp() {
    monitoringSourceIdentifier = generateUuid();
    monitoringSourceName = "source-name";
    accountId = generateUuid();
    projectIdentifier = "harness";
    productName = "Performance monitoring";
    envIdentifier = "production_harness";
    serviceIdentifier = "manager";
  }

  protected void fillCommonFields(DSConfig dsConfig) {
    dsConfig.setAccountId(accountId);
    dsConfig.setConnectorIdentifier(connectorIdentifier);
    dsConfig.setIdentifier(monitoringSourceIdentifier);
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
    cvConfig.setIdentifier(monitoringSourceIdentifier);
    cvConfig.setMonitoringSourceName(monitoringSourceName);
    cvConfig.setServiceIdentifier(serviceIdentifier);
  }

  protected void assertCommon(CVConfig cvConfig, DSConfig dsConfig) {
    assertThat(cvConfig.getAccountId()).isEqualTo(dsConfig.getAccountId());
    assertThat(cvConfig.getIdentifier()).isEqualTo(dsConfig.getIdentifier());
    assertThat(cvConfig.getMonitoringSourceName()).isEqualTo(dsConfig.getMonitoringSourceName());
    assertThat(cvConfig.getProjectIdentifier()).isEqualTo(dsConfig.getProjectIdentifier());
    assertThat(cvConfig.getConnectorIdentifier()).isEqualTo(dsConfig.getConnectorIdentifier());
    assertThat(cvConfig.getProductName()).isEqualTo(dsConfig.getProductName());
  }
}
