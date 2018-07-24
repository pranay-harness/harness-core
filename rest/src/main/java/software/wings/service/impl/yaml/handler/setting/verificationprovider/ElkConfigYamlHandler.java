package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import com.google.inject.Singleton;

import software.wings.beans.ElkConfig;
import software.wings.beans.ElkConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.utils.Util;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class ElkConfigYamlHandler extends VerificationProviderYamlHandler<Yaml, ElkConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    ElkConfig config = (ElkConfig) settingAttribute.getValue();
    String connectorType = Util.getStringFromEnum(config.getElkConnector());

    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(config.getType())
        .elkUrl(config.getElkUrl())
        .username(config.getUsername())
        .password(getEncryptedValue(config, "password", false))
        .connectorType(connectorType)
        .validationType(config.getValidationType())
        .build();
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    ElkConnector elkConnector = Util.getEnumFromString(ElkConnector.class, yaml.getConnectorType());
    ElkConfig config = ElkConfig.builder()
                           .accountId(accountId)
                           .elkUrl(yaml.getElkUrl())
                           .encryptedPassword(yaml.getPassword())
                           .validationType(yaml.getValidationType())
                           .username(yaml.getUsername())
                           .elkConnector(elkConnector)
                           .build();

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
