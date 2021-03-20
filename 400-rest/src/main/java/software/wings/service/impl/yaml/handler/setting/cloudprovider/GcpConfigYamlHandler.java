package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import software.wings.beans.GcpConfig;
import software.wings.beans.GcpConfigYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class GcpConfigYamlHandler extends CloudProviderYamlHandler<GcpConfigYaml, GcpConfig> {
  @Override
  public GcpConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    GcpConfig gcpConfig = (GcpConfig) settingAttribute.getValue();

    GcpConfigYaml yaml = GcpConfigYaml.builder()
                             .harnessApiVersion(getHarnessApiVersion())
                             .type(gcpConfig.getType())
                             .serviceAccountKeyFileContent(getEncryptedYamlRef(
                                 gcpConfig.getAccountId(), gcpConfig.getEncryptedServiceAccountKeyFileContent()))
                             .useDelegate(gcpConfig.isUseDelegate())
                             .delegateSelector(gcpConfig.getDelegateSelector())
                             .skipValidation(gcpConfig.isSkipValidation())
                             .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  public SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<GcpConfigYaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    GcpConfigYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    GcpConfig config = GcpConfig.builder()
                           .accountId(accountId)
                           .encryptedServiceAccountKeyFileContent(yaml.getServiceAccountKeyFileContent())
                           .useDelegate(yaml.isUseDelegate())
                           .delegateSelector(yaml.getDelegateSelector())
                           .skipValidation(yaml.isSkipValidation())
                           .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return GcpConfigYaml.class;
  }
}
