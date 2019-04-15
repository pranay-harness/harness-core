package software.wings.helpers.ext.helm.request;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.settings.SettingValue;

import java.util.List;

@Data
@Builder
public class HelmChartConfigParams {
  private HelmRepoConfig helmRepoConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private String repoDisplayName;
  private String repoName;

  private SettingValue connectorConfig;
  private List<EncryptedDataDetail> connectorEncryptedDataDetails;

  private String chartName;
  private String chartVersion;
}
