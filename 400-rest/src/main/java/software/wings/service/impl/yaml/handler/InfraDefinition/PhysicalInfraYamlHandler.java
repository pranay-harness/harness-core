package software.wings.service.impl.yaml.handler.InfraDefinition;

import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.PhysicalInfra;
import software.wings.infra.PhysicalInfraYaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class PhysicalInfraYamlHandler extends CloudProviderInfrastructureYamlHandler<PhysicalInfraYaml, PhysicalInfra> {
  @Inject private SettingsService settingsService;
  @Override
  public PhysicalInfraYaml toYaml(PhysicalInfra bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    SettingAttribute hostNameConnectionAttr = settingsService.get(bean.getHostConnectionAttrs());
    return PhysicalInfraYaml.builder()
        .hosts(bean.getHosts())
        .hostNames(bean.getHostNames())
        .hostConnectionAttrsName(hostNameConnectionAttr.getName())
        .loadBalancerName(bean.getLoadBalancerName())
        .cloudProviderName(cloudProvider.getName())
        .type(InfrastructureType.PHYSICAL_INFRA)
        .expressions(bean.getExpressions())
        .build();
  }

  @Override
  public PhysicalInfra upsertFromYaml(
      ChangeContext<PhysicalInfraYaml> changeContext, List<ChangeContext> changeSetContext) {
    PhysicalInfra bean = PhysicalInfra.builder().build();
    toBean(bean, changeContext);
    return bean;
  }

  private void toBean(PhysicalInfra bean, ChangeContext<PhysicalInfraYaml> changeContext) {
    PhysicalInfraYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    SettingAttribute hostConnectionAttr =
        settingsService.getSettingAttributeByName(accountId, yaml.getHostConnectionAttrsName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setHosts(yaml.getHosts());
    bean.setHostNames(yaml.getHostNames());
    bean.setHostConnectionAttrs(hostConnectionAttr.getUuid());
    bean.setLoadBalancerName(yaml.getLoadBalancerName());
    bean.setExpressions(yaml.getExpressions());
  }

  @Override
  public Class getYamlClass() {
    return PhysicalInfraYaml.class;
  }
}
