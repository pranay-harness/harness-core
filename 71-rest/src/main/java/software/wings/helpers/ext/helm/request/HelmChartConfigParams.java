package software.wings.helpers.ext.helm.request;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import software.wings.settings.SettingValue;

import java.util.List;

@Data
@Builder
public class HelmChartConfigParams implements ExecutionCapabilityDemander {
  private HelmRepoConfig helmRepoConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private String repoDisplayName;
  private String repoName;

  private SettingValue connectorConfig;
  private List<EncryptedDataDetail> connectorEncryptedDataDetails;

  private String chartName;
  private String chartVersion;
  private String chartUrl;
  private String basePath;

  private HelmVersion helmVersion;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.generateDelegateCapabilities(helmRepoConfig, encryptedDataDetails);
  }
}
