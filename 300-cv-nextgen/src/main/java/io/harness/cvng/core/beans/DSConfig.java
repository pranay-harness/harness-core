/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.beans;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.CVConfig;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

@Data
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class DSConfig {
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  private String productName;
  private String connectorIdentifier;
  private String identifier;
  private String monitoringSourceName;

  public abstract DataSourceType getType();
  public abstract CVConfigUpdateResult getCVConfigUpdateResult(List<CVConfig> existingCVConfigs);
  public abstract void validate(List<CVConfig> existingMapping);

  @Value
  @Builder
  public static class CVConfigUpdateResult {
    @Builder.Default List<CVConfig> updated = new ArrayList<>();
    @Builder.Default List<CVConfig> deleted = new ArrayList<>();
    @Builder.Default List<CVConfig> added = new ArrayList<>();
  }
  public void populateCommonFields(CVConfig cvConfig) {
    this.accountId = cvConfig.getAccountId();
    this.orgIdentifier = cvConfig.getOrgIdentifier();
    this.projectIdentifier = cvConfig.getProjectIdentifier();
    this.productName = cvConfig.getProductName();
    this.connectorIdentifier = cvConfig.getConnectorIdentifier();
    this.identifier = cvConfig.getIdentifier();
    this.monitoringSourceName = cvConfig.getMonitoringSourceName();
  }

  protected void fillCommonFields(CVConfig cvConfig) {
    cvConfig.setAccountId(accountId);
    cvConfig.setOrgIdentifier(orgIdentifier);
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setProductName(productName);
    cvConfig.setConnectorIdentifier(connectorIdentifier);
    cvConfig.setIdentifier(identifier);
    cvConfig.setMonitoringSourceName(monitoringSourceName);
  }
}
