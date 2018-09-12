package software.wings.service.impl.yaml.handler.deploymentspec.container;

import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.api.DeploymentType;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.HelmChartSpecification.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.deploymentspec.DeploymentSpecificationYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;

@Singleton
public class HelmChartSpecificationYamlHandler
    extends DeploymentSpecificationYamlHandler<Yaml, HelmChartSpecification> {
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private YamlHelper yamlHelper;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public Yaml toYaml(HelmChartSpecification bean, String appId) {
    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(DeploymentType.HELM.name())
        .chartUrl(bean.getChartUrl())
        .chartName(bean.getChartName())
        .chartVersion(bean.getChartVersion())
        .build();
  }

  @Override
  public HelmChartSpecification upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    HelmChartSpecification previous =
        get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    HelmChartSpecification helmChartSpecification = toBean(changeContext);
    helmChartSpecification.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    if (previous != null) {
      helmChartSpecification.setUuid(previous.getUuid());
      return serviceResourceService.updateHelmChartSpecification(helmChartSpecification);
    } else {
      return serviceResourceService.createHelmChartSpecification(helmChartSpecification);
    }
  }

  private HelmChartSpecification toBean(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();

    String filePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), filePath);
    notNullCheck("Could not lookup app for the yaml file: " + filePath, appId, USER);

    String serviceId = yamlHelper.getServiceId(appId, filePath);
    notNullCheck("Could not lookup service for the yaml file: " + filePath, serviceId, USER);

    HelmChartSpecification helmChartSpecification = HelmChartSpecification.builder()
                                                        .chartName(yaml.getChartName())
                                                        .chartUrl(yaml.getChartUrl())
                                                        .chartVersion(yaml.getChartVersion())
                                                        .serviceId(serviceId)
                                                        .build();
    helmChartSpecification.setAppId(appId);
    return helmChartSpecification;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public HelmChartSpecification get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Could not lookup app for the yaml file: " + yamlFilePath, appId, USER);

    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    notNullCheck("Could not lookup service for the yaml file: " + yamlFilePath, serviceId, USER);

    return serviceResourceService.getHelmChartSpecification(appId, serviceId);
  }
}
