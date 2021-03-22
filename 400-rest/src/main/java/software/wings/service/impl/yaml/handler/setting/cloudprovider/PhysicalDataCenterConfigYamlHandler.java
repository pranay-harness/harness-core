package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.PhysicalDataCenterConfigYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class PhysicalDataCenterConfigYamlHandler
    extends CloudProviderYamlHandler<PhysicalDataCenterConfigYaml, PhysicalDataCenterConfig> {
  @Override
  public PhysicalDataCenterConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    PhysicalDataCenterConfig physicalDataCenterConfig = (PhysicalDataCenterConfig) settingAttribute.getValue();

    PhysicalDataCenterConfigYaml yaml = PhysicalDataCenterConfigYaml.builder()
                                            .harnessApiVersion(getHarnessApiVersion())
                                            .type(physicalDataCenterConfig.getType())
                                            .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous,
      ChangeContext<PhysicalDataCenterConfigYaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    String accountId = changeContext.getChange().getAccountId();
    PhysicalDataCenterConfig config = new PhysicalDataCenterConfig();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return PhysicalDataCenterConfigYaml.class;
  }
}
