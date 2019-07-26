package software.wings.service.impl.yaml.handler.InfraDefinition;

import static java.lang.String.format;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.AzureInstanceInfrastructure;
import software.wings.infra.AzureInstanceInfrastructure.Yaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.intfc.SettingsService;

import java.util.List;

@Singleton
public class AzureInstanceInfrastructureYamlHandler
    extends CloudProviderInfrastructureYamlHandler<Yaml, AzureInstanceInfrastructure> {
  @Inject private SettingsService settingsService;
  @Override
  public Yaml toYaml(AzureInstanceInfrastructure bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    return Yaml.builder()
        .resourceGroup(bean.getResourceGroup())
        .subscriptionId(bean.getSubscriptionId())
        .tags(bean.getTags())
        .cloudProviderName(cloudProvider.getName())
        .type(InfrastructureType.AZURE_SSH)
        .build();
  }

  @Override
  public AzureInstanceInfrastructure upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    AzureInstanceInfrastructure bean = AzureInstanceInfrastructure.builder().build();
    toBean(bean, changeContext);
    return bean;
  }

  private void toBean(AzureInstanceInfrastructure bean, ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setResourceGroup(yaml.getResourceGroup());
    bean.setSubscriptionId(yaml.getSubscriptionId());
    bean.setTags(yaml.getTags());
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
