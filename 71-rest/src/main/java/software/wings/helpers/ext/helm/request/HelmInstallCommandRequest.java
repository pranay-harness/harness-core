package software.wings.helpers.ext.helm.request;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.command.LogCallback;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 3/22/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class HelmInstallCommandRequest extends HelmCommandRequest {
  private Integer newReleaseVersion;
  private Integer prevReleaseVersion;
  private String namespace;
  private long timeoutInMillis;
  private Map<String, String> valueOverrides;
  private List<String> variableOverridesYamlFiles;
  private GitFileConfig gitFileConfig;
  private String workingDir;

  public HelmInstallCommandRequest() {
    super(HelmCommandType.INSTALL);
  }

  @Builder
  public HelmInstallCommandRequest(String accountId, String appId, String kubeConfigLocation, String commandName,
      String activityId, ContainerServiceParams containerServiceParams, String releaseName,
      HelmChartSpecification chartSpecification, int newReleaseVersion, int prevReleaseVersion, String namespace,
      long timeoutInMillis, Map<String, String> valueOverrides, List<String> variableOverridesYamlFiles,
      String repoName, GitConfig gitConfig, GitFileConfig gitFileConfig, List<EncryptedDataDetail> encryptedDataDetails,
      LogCallback executionLogCallback, String commandFlags, K8sDelegateManifestConfig sourceRepoConfig,
      HelmVersion helmVersion) {
    super(HelmCommandType.INSTALL, accountId, appId, kubeConfigLocation, commandName, activityId,
        containerServiceParams, releaseName, chartSpecification, repoName, gitConfig, encryptedDataDetails,
        executionLogCallback, commandFlags, sourceRepoConfig, helmVersion);
    this.newReleaseVersion = newReleaseVersion;
    this.prevReleaseVersion = prevReleaseVersion;
    this.namespace = namespace;
    this.timeoutInMillis = timeoutInMillis;
    this.valueOverrides = valueOverrides;
    this.variableOverridesYamlFiles = variableOverridesYamlFiles;
    this.gitFileConfig = gitFileConfig;
  }
}
