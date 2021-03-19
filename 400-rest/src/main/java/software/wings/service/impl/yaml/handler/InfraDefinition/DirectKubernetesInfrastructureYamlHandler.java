package software.wings.service.impl.yaml.handler.InfraDefinition;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.DirectKubernetesInfrastructure;
import software.wings.infra.DirectKubernetesInfrastructureYaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import java.util.List;

public class DirectKubernetesInfrastructureYamlHandler
    extends CloudProviderInfrastructureYamlHandler<DirectKubernetesInfrastructureYaml, DirectKubernetesInfrastructure> {
  @Inject private SettingsService settingsService;
  @Override
  public DirectKubernetesInfrastructureYaml toYaml(DirectKubernetesInfrastructure bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    DirectKubernetesInfrastructureYaml yaml = DirectKubernetesInfrastructureYaml.builder()
                                                  .clusterName(bean.getClusterName())
                                                  .namespace(bean.getNamespace())
                                                  .releaseName(bean.getReleaseName())
                                                  .cloudProviderName(cloudProvider.getName())
                                                  .type(InfrastructureType.DIRECT_KUBERNETES)
                                                  .expressions(bean.getExpressions())
                                                  .build();

    // To prevent default release name from showing in yaml when provisioner
    if (isNotEmpty(bean.getExpressions())) {
      yaml.setReleaseName(null);
    }
    return yaml;
  }

  @Override
  public DirectKubernetesInfrastructure upsertFromYaml(
      ChangeContext<DirectKubernetesInfrastructureYaml> changeContext, List<ChangeContext> changeSetContext) {
    DirectKubernetesInfrastructure bean = DirectKubernetesInfrastructure.builder().build();
    toBean(bean, changeContext);
    return bean;
  }

  private void toBean(
      DirectKubernetesInfrastructure bean, ChangeContext<DirectKubernetesInfrastructureYaml> changeContext) {
    DirectKubernetesInfrastructureYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setClusterName(yaml.getClusterName());
    bean.setNamespace(yaml.getNamespace());
    bean.setReleaseName(yaml.getReleaseName());
    bean.setExpressions(yaml.getExpressions());
  }

  @Override
  public Class getYamlClass() {
    return DirectKubernetesInfrastructureYaml.class;
  }
}
