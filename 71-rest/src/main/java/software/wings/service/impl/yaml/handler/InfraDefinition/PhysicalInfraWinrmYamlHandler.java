package software.wings.service.impl.yaml.handler.InfraDefinition;

import static java.lang.String.format;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.PhysicalInfraWinrm;
import software.wings.infra.PhysicalInfraWinrm.Yaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.SettingsService;

import java.util.List;

@Singleton
public class PhysicalInfraWinrmYamlHandler extends CloudProviderInfrastructureYamlHandler<Yaml, PhysicalInfraWinrm> {
  @Inject private YamlHelper yamlHelper;
  @Inject private SettingsService settingsService;
  @Override
  public Yaml toYaml(PhysicalInfraWinrm bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    return Yaml.builder()
        .hosts(bean.getHosts())
        .hostNames(bean.getHostNames())
        .hostConnectionAttrs(bean.getWinRmConnectionAttributes())
        .loadBalancerName(bean.getLoadBalancerId())
        .cloudProviderName(cloudProvider.getName())
        .type(InfrastructureType.PHYSICAL_INFRA_WINRM)
        .build();
  }

  @Override
  public PhysicalInfraWinrm upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    PhysicalInfraWinrm physicalInfraWinrm = PhysicalInfraWinrm.builder().build();
    toBean(physicalInfraWinrm, changeContext);
    return physicalInfraWinrm;
  }

  private void toBean(PhysicalInfraWinrm bean, ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setHosts(yaml.getHosts());
    bean.setHostNames(yaml.getHostNames());
    bean.setWinRmConnectionAttributes(yaml.getHostConnectionAttrs());
    bean.setLoadBalancerName(yaml.getLoadBalancerName());
    bean.setLoadBalancerId(yaml.getLoadBalancerName());
  }

  public Class getYamlClass() {
    return Yaml.class;
  }
}
