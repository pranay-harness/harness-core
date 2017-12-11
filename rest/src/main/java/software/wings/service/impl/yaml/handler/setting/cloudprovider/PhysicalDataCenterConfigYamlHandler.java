package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.PhysicalDataCenterConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class PhysicalDataCenterConfigYamlHandler extends CloudProviderYamlHandler<Yaml, PhysicalDataCenterConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    PhysicalDataCenterConfig physicalDataCenterConfig = (PhysicalDataCenterConfig) settingAttribute.getValue();
    return new Yaml(physicalDataCenterConfig.getType());
  }

  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    String accountId = changeContext.getChange().getAccountId();
    PhysicalDataCenterConfig config = new PhysicalDataCenterConfig();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
