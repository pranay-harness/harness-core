package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import software.wings.beans.JenkinsConfig;
import software.wings.beans.JenkinsConfig.VerificationYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
public class JenkinsConfigVerificationYamlHandler
    extends VerificationProviderYamlHandler<VerificationYaml, JenkinsConfig> {
  @Override
  public VerificationYaml toYaml(SettingAttribute settingAttribute, String appId) {
    JenkinsConfig jenkinsConfig = (JenkinsConfig) settingAttribute.getValue();
    return new VerificationYaml(jenkinsConfig.getType(), jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(),
        getEncryptedValue(jenkinsConfig, "password", false));
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<VerificationYaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    VerificationYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    JenkinsConfig config = JenkinsConfig.builder()
                               .accountId(accountId)
                               .jenkinsUrl(yaml.getUrl())
                               .encryptedPassword(yaml.getPassword())
                               .username(yaml.getUsername())
                               .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return VerificationYaml.class;
  }
}
