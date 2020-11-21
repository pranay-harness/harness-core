package software.wings.helpers.ext.helm.request;

import io.harness.k8s.model.HelmVersion;
import io.harness.logging.LogCallback;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by anubhaw on 4/2/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class HelmReleaseHistoryCommandRequest extends HelmCommandRequest {
  public HelmReleaseHistoryCommandRequest() {
    super(HelmCommandType.RELEASE_HISTORY);
  }

  @Builder
  public HelmReleaseHistoryCommandRequest(String accountId, String appId, String kubeConfigLocation, String commandName,
      String activityId, ContainerServiceParams containerServiceParams, String releaseName, GitConfig gitConfig,
      List<EncryptedDataDetail> encryptedDataDetails, LogCallback executionLogCallback, String commandFlags,
      HelmVersion helmVersion, String ocPath, String workingDir, List<String> variableOverridesYamlFiles,
      GitFileConfig gitFileConfig, boolean k8SteadyStateCheckEnabled, boolean deprecateFabric8Enabled) {
    super(HelmCommandType.RELEASE_HISTORY, accountId, appId, kubeConfigLocation, commandName, activityId,
        containerServiceParams, releaseName, null, null, gitConfig, encryptedDataDetails, executionLogCallback,
        commandFlags, null, helmVersion, ocPath, workingDir, variableOverridesYamlFiles, gitFileConfig,
        k8SteadyStateCheckEnabled, deprecateFabric8Enabled);
  }
}
