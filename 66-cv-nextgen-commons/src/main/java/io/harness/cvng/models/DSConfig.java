package io.harness.cvng.models;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.cvng.core.services.entities.CVConfig;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class DSConfig {
  private String identifier;
  private String accountId;
  private String projectIdentifier;
  private String productName;
  private String connectorId;
  private String envIdentifier;

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
    this.projectIdentifier = cvConfig.getProjectIdentifier();
    this.productName = cvConfig.getProductName();
    this.connectorId = cvConfig.getConnectorId();
    this.envIdentifier = cvConfig.getEnvIdentifier();
  }

  protected void fillCommonFields(CVConfig cvConfig) {
    cvConfig.setGroupId(identifier);
    cvConfig.setAccountId(accountId);
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setProductName(productName);
    cvConfig.setConnectorId(connectorId);
    cvConfig.setEnvIdentifier(envIdentifier);
  }
}
