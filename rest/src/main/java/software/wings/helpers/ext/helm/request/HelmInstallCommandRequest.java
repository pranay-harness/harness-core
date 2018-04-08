package software.wings.helpers.ext.helm.request;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.service.impl.ContainerServiceParams;

import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 3/22/18.
 */
@Data
public class HelmInstallCommandRequest extends HelmCommandRequest {
  private HelmChartSpecification chartSpecification;
  private String releaseName;
  private String releaseVersion;
  private String harnessVersion;
  private String namespace;
  private long timeoutInMillis;
  private Map<String, String> valueOverrides;
  private List<String> variableOverridesYamlFiles;

  public HelmInstallCommandRequest() {
    super(HelmCommandType.INSTALL);
  }

  @Builder
  public HelmInstallCommandRequest(String accountId, String appId, String kubeConfigLocation, String commandName,
      String activityId, ContainerServiceParams containerServiceParams, HelmChartSpecification chartSpecification,
      String releaseName, String releaseVersion, String harnessVersion, String namespace, long timeoutInMillis,
      Map<String, String> valueOverrides, List<String> variableOverridesYamlFiles) {
    super(
        HelmCommandType.INSTALL, accountId, appId, kubeConfigLocation, commandName, activityId, containerServiceParams);
    this.chartSpecification = chartSpecification;
    this.releaseName = releaseName;
    this.releaseVersion = releaseVersion;
    this.harnessVersion = harnessVersion;
    this.namespace = namespace;
    this.timeoutInMillis = timeoutInMillis;
    this.valueOverrides = valueOverrides;
    this.variableOverridesYamlFiles = variableOverridesYamlFiles;
  }
}
