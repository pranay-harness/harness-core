package software.wings.service.impl.yaml.handler.setting.cloudprovider;

import software.wings.beans.GcpConfig;
import software.wings.beans.GcpConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.io.IOException;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class GcpConfigYamlHandler extends CloudProviderYamlHandler<Yaml, GcpConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    GcpConfig gcpConfig = (GcpConfig) settingAttribute.getValue();

    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(gcpConfig.getType())
        .serviceAccountKeyFileContent(getEncryptedValue(gcpConfig, "serviceAccountKeyFileContent", false))
        .build();
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    char[] decryptedFileContent;
    try {
      decryptedFileContent = secretManager.decryptYamlRef(yaml.getServiceAccountKeyFileContent());
    } catch (IllegalAccessException | IOException e) {
      throw new HarnessException(
          "Exception while decrypting the file content ref:" + yaml.getServiceAccountKeyFileContent());
    }

    GcpConfig config = GcpConfig.builder()
                           .accountId(accountId)
                           .serviceAccountKeyFileContent(decryptedFileContent)
                           .encryptedServiceAccountKeyFileContent(yaml.getServiceAccountKeyFileContent())
                           .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
