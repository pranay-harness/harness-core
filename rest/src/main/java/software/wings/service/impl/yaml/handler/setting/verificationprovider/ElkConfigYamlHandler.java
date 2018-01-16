package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import com.google.inject.Singleton;

import software.wings.beans.ElkConfig;
import software.wings.beans.ElkConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.utils.Util;

import java.io.IOException;
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
        .build();
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    ElkConfig config = new ElkConfig();
    config.setAccountId(accountId);
    config.setElkUrl(yaml.getElkUrl());
    config.setEncryptedPassword(yaml.getPassword());

    char[] decryptedPassword;
    try {
      decryptedPassword = secretManager.decryptYamlRef(yaml.getPassword());
    } catch (IllegalAccessException | IOException e) {
      throw new HarnessException("Exception while decrypting the password ref:" + yaml.getPassword());
    }

    config.setPassword(decryptedPassword);
    config.setUsername(yaml.getUsername());
    ElkConnector elkConnector = Util.getEnumFromString(ElkConnector.class, yaml.getConnectorType());
    config.setElkConnector(elkConnector);

    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
