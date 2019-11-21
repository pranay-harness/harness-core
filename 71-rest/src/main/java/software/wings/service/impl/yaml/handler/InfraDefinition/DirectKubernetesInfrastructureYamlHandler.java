package software.wings.service.impl.yaml.handler.InfraDefinition;

import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;

import com.google.inject.Inject;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.DirectKubernetesInfrastructure;
import software.wings.infra.DirectKubernetesInfrastructure.Yaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.intfc.SettingsService;

import java.util.List;

public class DirectKubernetesInfrastructureYamlHandler
    extends CloudProviderInfrastructureYamlHandler<Yaml, DirectKubernetesInfrastructure> {
  @Inject private SettingsService settingsService;
  @Override
  public Yaml toYaml(DirectKubernetesInfrastructure bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    return Yaml.builder()
        .clusterName(bean.getClusterName())
        .namespace(bean.getNamespace())
        .releaseName(bean.getReleaseName())
        .cloudProviderName(cloudProvider.getName())
        .type(InfrastructureType.DIRECT_KUBERNETES)
        .build();
  }

  @Override
  public DirectKubernetesInfrastructure upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    DirectKubernetesInfrastructure bean = DirectKubernetesInfrastructure.builder().build();
    toBean(bean, changeContext);
    return bean;
  }

  private void toBean(DirectKubernetesInfrastructure bean, ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setClusterName(yaml.getClusterName());
    bean.setNamespace(yaml.getNamespace());
    bean.setReleaseName(yaml.getReleaseName());
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
