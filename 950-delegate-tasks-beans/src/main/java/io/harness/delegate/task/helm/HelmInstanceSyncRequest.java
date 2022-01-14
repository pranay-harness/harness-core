package io.harness.delegate.task.helm;

import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;
import io.harness.k8s.model.HelmVersion;
import io.harness.logging.LogCallback;

import java.util.List;
import lombok.Builder;

public class HelmInstanceSyncRequest extends HelmCommandRequestNG {
  @Builder
  public HelmInstanceSyncRequest(String releaseName, HelmCommandType helmCommandType, List<String> valuesYamlList,
      K8sInfraDelegateConfig k8sInfraDelegateConfig, ManifestDelegateConfig manifestDelegateConfig, String accountId,
      boolean k8SteadyStateCheckEnabled, boolean shouldOpenFetchFilesLogStream,
      CommandUnitsProgress commandUnitsProgress, LogCallback logCallback, String namespace, HelmVersion helmVersion,
      String commandFlags, String repoName, String workingDir, String kubeConfigLocation, String ocPath,
      String commandName, boolean useLatestKubectlVersion) {
    super(releaseName, helmCommandType, valuesYamlList, k8sInfraDelegateConfig, manifestDelegateConfig, accountId,
        k8SteadyStateCheckEnabled, shouldOpenFetchFilesLogStream, commandUnitsProgress, logCallback, namespace,
        helmVersion, commandFlags, repoName, workingDir, kubeConfigLocation, ocPath, commandName,
        useLatestKubectlVersion);
  }
}
