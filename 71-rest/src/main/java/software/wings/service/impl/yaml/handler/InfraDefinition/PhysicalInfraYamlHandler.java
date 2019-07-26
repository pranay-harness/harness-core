package software.wings.service.impl.yaml.handler.InfraDefinition;

import static java.lang.String.format;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.PhysicalInfra;
import software.wings.infra.PhysicalInfra.Yaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.intfc.SettingsService;

import java.util.List;

@Singleton
public class PhysicalInfraYamlHandler extends CloudProviderInfrastructureYamlHandler<Yaml, PhysicalInfra> {
  @Inject private SettingsService settingsService;
  @Override
  public Yaml toYaml(PhysicalInfra bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    return Yaml.builder()
        .hosts(bean.getHosts())
        .hostNames(bean.getHostNames())
        .hostConnectionAttrs(bean.getHostConnectionAttrs())
        .loadBalancerName(bean.getLoadBalancerName())
        .cloudProviderName(cloudProvider.getName())
        .type(InfrastructureType.PHYSICAL_INFRA)
        .build();
  }

  @Override
  public PhysicalInfra upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    PhysicalInfra bean = PhysicalInfra.builder().build();
    toBean(bean, changeContext);
    return bean;
  }

  private void toBean(PhysicalInfra bean, ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setHosts(yaml.getHosts());
    bean.setHostNames(yaml.getHostNames());
    bean.setHostConnectionAttrs(yaml.getHostConnectionAttrs());
    bean.setLoadBalancerName(yaml.getLoadBalancerName());
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
