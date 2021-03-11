package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AppDynamicsConfigYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class AppDynamicsConfigYamlHandler
    extends VerificationProviderYamlHandler<AppDynamicsConfigYaml, AppDynamicsConfig> {
  @Override
  public AppDynamicsConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    AppDynamicsConfig config = (AppDynamicsConfig) settingAttribute.getValue();
    AppDynamicsConfigYaml yaml =
        AppDynamicsConfigYaml.builder()
            .harnessApiVersion(getHarnessApiVersion())
            .type(config.getType())
            .username(config.getUsername())
            .password(getEncryptedYamlRef(config.getAccountId(), config.getEncryptedPassword()))
            .accountName(config.getAccountname())
            .controllerUrl(config.getControllerUrl())
            .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<AppDynamicsConfigYaml> changeContext,
      List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    AppDynamicsConfigYaml yaml = changeContext.getYaml();
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
    return AppDynamicsConfigYaml.class;
  }
}
