package io.harness.delegate.task.helm;

import io.harness.delegate.task.k8s.HelmChartManifestDelegateConfig;
import io.harness.helm.HelmCommandData;

import com.google.inject.Singleton;
import java.util.Optional;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@Singleton
@UtilityClass
public class HelmCommandDataMapperNG {
  public HelmCommandData getHelmCmdDataNG(@NonNull HelmCommandRequestNG helmCommandRequestNG) {
    HelmChartManifestDelegateConfig helmChartManifestDelegateConfig =
        (HelmChartManifestDelegateConfig) helmCommandRequestNG.getManifestDelegateConfig();

    HelmCommandData helmCommandData =
        HelmCommandData.builder()
            .kubeConfigLocation(Optional.ofNullable(helmCommandRequestNG.getKubeConfigLocation()).orElse(""))
            .helmVersion(helmCommandRequestNG.getHelmVersion())
            .releaseName(helmCommandRequestNG.getReleaseName())
            .isRepoConfigNull(helmCommandRequestNG.getManifestDelegateConfig() == null)
            .yamlFiles(helmCommandRequestNG.getValuesYamlList())
            .logCallback(helmCommandRequestNG.getLogCallback())
            .workingDir(helmCommandRequestNG.getWorkingDir())
            .commandFlags(helmCommandRequestNG.getCommandFlags())
            .repoName(helmCommandRequestNG.getRepoName())
            .namespace(helmCommandRequestNG.getNamespace())
            .valueMap(helmChartManifestDelegateConfig.getHelmCommandFlag().getValueMap())
            .isHelmCmdFlagsNull(helmChartManifestDelegateConfig.getHelmCommandFlag() == null)
            .build();

    if (helmCommandRequestNG instanceof HelmInstallCommandRequestNG) {
      HelmInstallCommandRequestNG helmInstallCommandRequest = (HelmInstallCommandRequestNG) helmCommandRequestNG;
      helmCommandData.setPrevReleaseVersion(helmInstallCommandRequest.getPrevReleaseVersion());
      helmCommandData.setNewReleaseVersion(helmInstallCommandRequest.getNewReleaseVersion());
    }

    if (helmCommandRequestNG instanceof HelmRollbackCommandRequestNG) {
      HelmRollbackCommandRequestNG helmRollbackCommandRequest = (HelmRollbackCommandRequestNG) helmCommandRequestNG;
      helmCommandData.setPrevReleaseVersion(helmRollbackCommandRequest.getPrevReleaseVersion());
      helmCommandData.setNewReleaseVersion(helmRollbackCommandRequest.getNewReleaseVersion());
    }

    return helmCommandData;
  }
}
