package software.wings.service.impl.yaml.handler.InfraDefinition;

import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.AzureKubernetesService;
import software.wings.infra.AzureKubernetesServiceYaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class AzureKubernetesServiceYamlHandler
    extends CloudProviderInfrastructureYamlHandler<AzureKubernetesServiceYaml, AzureKubernetesService> {
  @Inject private SettingsService settingsService;
  @Override
  public AzureKubernetesServiceYaml toYaml(AzureKubernetesService bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    return AzureKubernetesServiceYaml.builder()
        .clusterName(bean.getClusterName())
        .namespace(bean.getNamespace())
        .releaseName(bean.getReleaseName())
        .resourceGroup(bean.getResourceGroup())
        .subscriptionId(bean.getSubscriptionId())
        .cloudProviderName(cloudProvider.getName())
        .type(InfrastructureType.AZURE_KUBERNETES)
        .build();
  }

  @Override
  public AzureKubernetesService upsertFromYaml(
      ChangeContext<AzureKubernetesServiceYaml> changeContext, List<ChangeContext> changeSetContext) {
    AzureKubernetesService bean = AzureKubernetesService.builder().build();
    toBean(bean, changeContext);
    return bean;
  }

  private void toBean(AzureKubernetesService bean, ChangeContext<AzureKubernetesServiceYaml> changeContext) {
    AzureKubernetesServiceYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    bean.setClusterName(yaml.getClusterName());
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setNamespace(yaml.getNamespace());
    bean.setReleaseName(yaml.getReleaseName());
    bean.setResourceGroup(yaml.getResourceGroup());
    bean.setSubscriptionId(yaml.getSubscriptionId());
  }

  @Override
  public Class getYamlClass() {
    return AzureKubernetesServiceYaml.class;
  }
}
