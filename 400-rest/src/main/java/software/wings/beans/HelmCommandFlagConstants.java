package software.wings.beans;

import static io.harness.k8s.model.HelmVersion.V2;
import static io.harness.k8s.model.HelmVersion.V3;

import static software.wings.beans.appmanifest.StoreType.HelmChartRepo;
import static software.wings.beans.appmanifest.StoreType.HelmSourceRepo;

import io.harness.helm.HelmCommandTemplateFactory.HelmCliCommandType;
import io.harness.k8s.model.HelmVersion;

import software.wings.api.DeploymentType;
import software.wings.beans.appmanifest.StoreType;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;

public final class HelmCommandFlagConstants {
  @Getter
  public enum HelmSubCommand {
    INSTALL(ImmutableSet.of(HelmCliCommandType.INSTALL.name()), ImmutableSet.of(V2, V3),
        ImmutableSet.of(DeploymentType.HELM), ImmutableSet.of(HelmSourceRepo, HelmChartRepo)),
    UPGRADE(ImmutableSet.of(HelmCliCommandType.UPGRADE.name()), ImmutableSet.of(V2, V3),
        ImmutableSet.of(DeploymentType.HELM), ImmutableSet.of(HelmSourceRepo, HelmChartRepo)),
    ROLLBACK(ImmutableSet.of(HelmCliCommandType.ROLLBACK.name()), ImmutableSet.of(V2, V3),
        ImmutableSet.of(DeploymentType.HELM), ImmutableSet.of(HelmSourceRepo, HelmChartRepo)),
    HISTORY(ImmutableSet.of(HelmCliCommandType.RELEASE_HISTORY.name()), ImmutableSet.of(V2, V3),
        ImmutableSet.of(DeploymentType.HELM), ImmutableSet.of(HelmSourceRepo, HelmChartRepo)),
    DELETE(ImmutableSet.of(HelmCliCommandType.DELETE_RELEASE.name()), ImmutableSet.of(V2),
        ImmutableSet.of(DeploymentType.HELM), ImmutableSet.of(HelmSourceRepo, HelmChartRepo)),
    UNINSTALL(ImmutableSet.of(HelmCliCommandType.DELETE_RELEASE.name()), ImmutableSet.of(V3),
        ImmutableSet.of(DeploymentType.HELM), ImmutableSet.of(HelmSourceRepo, HelmChartRepo)),
    LIST(ImmutableSet.of(HelmCliCommandType.LIST_RELEASE.name()), ImmutableSet.of(V2, V3),
        ImmutableSet.of(DeploymentType.HELM), ImmutableSet.of(HelmSourceRepo, HelmChartRepo)),
    VERSION(ImmutableSet.of(HelmCliCommandType.VERSION.name()), ImmutableSet.of(V2, V3),
        ImmutableSet.of(DeploymentType.HELM, DeploymentType.KUBERNETES),
        ImmutableSet.of(HelmSourceRepo, HelmChartRepo)),
    PULL(ImmutableSet.of(HelmCliCommandType.FETCH.name()), ImmutableSet.of(V3),
        ImmutableSet.of(DeploymentType.HELM, DeploymentType.KUBERNETES), ImmutableSet.of(HelmChartRepo)),
    FETCH(ImmutableSet.of(HelmCliCommandType.FETCH.name()), ImmutableSet.of(V2),
        ImmutableSet.of(DeploymentType.HELM, DeploymentType.KUBERNETES), ImmutableSet.of(HelmChartRepo)),
    TEMPLATE(
        ImmutableSet.of(HelmCliCommandType.RENDER_CHART.name(), HelmCliCommandType.RENDER_SPECIFIC_CHART_FILE.name()),
        ImmutableSet.of(V2, V3), ImmutableSet.of(DeploymentType.HELM, DeploymentType.KUBERNETES),
        ImmutableSet.of(HelmSourceRepo, HelmChartRepo));

    private final Set<String> commandTypes;
    private final Set<HelmVersion> helmVersions;
    private final Set<DeploymentType> deploymentTypes;
    private final Set<StoreType> storeTypes;

    HelmSubCommand(Set<String> commandTypes, Set<HelmVersion> helmVersions, Set<DeploymentType> deploymentTypes,
        Set<StoreType> storeTypes) {
      this.commandTypes = commandTypes;
      this.helmVersions = helmVersions;
      this.deploymentTypes = deploymentTypes;
      this.storeTypes = storeTypes;
    }
  }

  public static Set<HelmSubCommand> getFilteredHelmSubCommands(
      HelmVersion version, DeploymentType deploymentType, StoreType storeType) {
    return Arrays.stream(HelmSubCommand.values())
        .filter(sc
            -> sc.getHelmVersions().contains(version) && sc.getDeploymentTypes().contains(deploymentType)
                && sc.getStoreTypes().contains(storeType))
        .collect(Collectors.toSet());
  }

  public static Set<HelmSubCommand> getHelmSubCommands(HelmVersion version) {
    return Arrays.stream(HelmSubCommand.values())
        .filter(sc -> sc.getHelmVersions().contains(version))
        .collect(Collectors.toSet());
  }
}
