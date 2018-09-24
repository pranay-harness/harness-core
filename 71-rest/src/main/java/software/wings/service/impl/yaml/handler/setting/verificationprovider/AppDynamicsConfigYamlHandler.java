package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import com.google.inject.Singleton;

import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AppDynamicsConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class AppDynamicsConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, AppDynamicsConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    AppDynamicsConfig config = (AppDynamicsConfig) settingAttribute.getValue();
    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(config.getType())
        .username(config.getUsername())
        .password(getEncryptedValue(config, "password", false))
        .accountName(config.getAccountname())
        .controllerUrl(config.getControllerUrl())
        .build();
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    AppDynamicsConfig config = AppDynamicsConfig.builder()
                                   .accountId(accountId)
                                   .accountname(yaml.getAccountName())
                                   .controllerUrl(yaml.getControllerUrl())
                                   .encryptedPassword(yaml.getPassword())
                                   .username(yaml.getUsername())
                                   .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
