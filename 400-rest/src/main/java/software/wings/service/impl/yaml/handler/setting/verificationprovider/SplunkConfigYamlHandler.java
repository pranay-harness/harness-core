package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SplunkConfigYaml;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class SplunkConfigYamlHandler extends VerificationProviderYamlHandler<SplunkConfigYaml, SplunkConfig> {
  @Override
  public SplunkConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    SplunkConfig config = (SplunkConfig) settingAttribute.getValue();
    SplunkConfigYaml yaml = SplunkConfigYaml.builder()
                                .harnessApiVersion(getHarnessApiVersion())
                                .type(config.getType())
                                .splunkUrl(config.getSplunkUrl())
                                .username(config.getUsername())
                                .password(getEncryptedYamlRef(config.getAccountId(), config.getEncryptedPassword()))
                                .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<SplunkConfigYaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    SplunkConfigYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    SplunkConfig config = SplunkConfig.builder()
                              .accountId(accountId)
                              .splunkUrl(yaml.getSplunkUrl())
                              .encryptedPassword(yaml.getPassword())
                              .username(yaml.getUsername())
                              .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return SplunkConfigYaml.class;
  }
}
