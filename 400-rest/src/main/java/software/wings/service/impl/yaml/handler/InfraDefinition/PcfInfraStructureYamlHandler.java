package software.wings.service.impl.yaml.handler.InfraDefinition;

import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.PcfInfraStructure;
import software.wings.infra.PcfInfraStructureYaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import java.util.List;

public class PcfInfraStructureYamlHandler
    extends CloudProviderInfrastructureYamlHandler<PcfInfraStructureYaml, PcfInfraStructure> {
  @Inject private SettingsService settingsService;
  @Override
  public PcfInfraStructureYaml toYaml(PcfInfraStructure bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    return PcfInfraStructureYaml.builder()
        .organization(bean.getOrganization())
        .routeMaps(bean.getRouteMaps())
        .space(bean.getSpace())
        .tempRouteMap(bean.getTempRouteMap())
        .cloudProviderName(cloudProvider.getName())
        .type(InfrastructureType.PCF_INFRASTRUCTURE)
        .build();
  }

  @Override
  public PcfInfraStructure upsertFromYaml(
      ChangeContext<PcfInfraStructureYaml> changeContext, List<ChangeContext> changeSetContext) {
    PcfInfraStructure bean = PcfInfraStructure.builder().build();
    toBean(bean, changeContext);
    return bean;
  }

  private void toBean(PcfInfraStructure bean, ChangeContext<PcfInfraStructureYaml> changeContext) {
    PcfInfraStructureYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setOrganization(yaml.getOrganization());
    bean.setRouteMaps(yaml.getRouteMaps());
    bean.setSpace(yaml.getSpace());
    bean.setTempRouteMap(yaml.getTempRouteMap());
  }

  @Override
  public Class getYamlClass() {
    return PcfInfraStructureYaml.class;
  }
}
