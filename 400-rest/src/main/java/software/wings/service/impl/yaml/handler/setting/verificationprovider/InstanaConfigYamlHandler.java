package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.InstanaConfig;
import software.wings.beans.InstanaConfigYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class InstanaConfigYamlHandler extends VerificationProviderYamlHandler<InstanaConfigYaml, InstanaConfig> {
  @Override
  public InstanaConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    InstanaConfig config = (InstanaConfig) settingAttribute.getValue();
    InstanaConfigYaml yaml = InstanaConfigYaml.builder()
                                 .harnessApiVersion(getHarnessApiVersion())
                                 .type(config.getType())
                                 .instanaUrl(config.getInstanaUrl())
                                 .apiToken(getEncryptedYamlRef(config.getAccountId(), config.getEncryptedApiToken()))
                                 .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<InstanaConfigYaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    InstanaConfigYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    InstanaConfig config = InstanaConfig.builder()
                               .accountId(accountId)
                               .instanaUrl(yaml.getInstanaUrl())
                               .encryptedApiToken(yaml.getApiToken())

                               .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return InstanaConfigYaml.class;
  }
}
