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
  private String identifier;
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  private String productName;
  private String connectorIdentifier;
  private String envIdentifier;
  private String monitoringSourceIdentifier;
  private String monitoringSourceName;

  public abstract DataSourceType getType();
  public abstract CVConfigUpdateResult getCVConfigUpdateResult(List<CVConfig> existingCVConfigs);
  @Value
  @Builder
  public static class CVConfigUpdateResult {
    @Builder.Default List<CVConfig> updated = new ArrayList<>();
    @Builder.Default List<CVConfig> deleted = new ArrayList<>();
    @Builder.Default List<CVConfig> added = new ArrayList<>();
  }
  public void populateCommonFields(CVConfig cvConfig) {
    this.identifier = cvConfig.getGroupId();
    this.accountId = cvConfig.getAccountId();
    this.orgIdentifier = cvConfig.getOrgIdentifier();
    this.projectIdentifier = cvConfig.getProjectIdentifier();
    this.productName = cvConfig.getProductName();
    this.connectorIdentifier = cvConfig.getConnectorIdentifier();
    this.envIdentifier = cvConfig.getEnvIdentifier();
    this.monitoringSourceIdentifier = cvConfig.getMonitoringSourceIdentifier();
    this.monitoringSourceName = cvConfig.getMonitoringSourceName();
  }

  protected void fillCommonFields(CVConfig cvConfig) {
    cvConfig.setGroupId(identifier);
    cvConfig.setAccountId(accountId);
    cvConfig.setOrgIdentifier(orgIdentifier);
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setProductName(productName);
    cvConfig.setConnectorIdentifier(connectorIdentifier);
    cvConfig.setEnvIdentifier(envIdentifier);
    cvConfig.setMonitoringSourceIdentifier(monitoringSourceIdentifier);
    cvConfig.setMonitoringSourceName(monitoringSourceName);
  }
}
