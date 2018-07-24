package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Singleton;

import software.wings.beans.JenkinsConfig;
import software.wings.beans.JenkinsConfig.VerificationYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.common.Constants;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class JenkinsConfigVerificationYamlHandler
    extends VerificationProviderYamlHandler<VerificationYaml, JenkinsConfig> {
  @Override
  public VerificationYaml toYaml(SettingAttribute settingAttribute, String appId) {
    JenkinsConfig jenkinsConfig = (JenkinsConfig) settingAttribute.getValue();
    return VerificationYaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(jenkinsConfig.getType())
        .url(jenkinsConfig.getJenkinsUrl())
        .username(jenkinsConfig.getUsername())
        .password(getEncryptedValue(jenkinsConfig, "password", true))
        .token(jenkinsConfig.getEncryptedToken() != null ? getEncryptedValue(jenkinsConfig, "token", true) : null)
        .authMechanism(jenkinsConfig.getAuthMechanism())
        .build();
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<VerificationYaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    VerificationYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    if (isEmpty(yaml.getAuthMechanism())) {
      yaml.setAuthMechanism(Constants.USERNAME_PASSWORD_FIELD);
    }
    JenkinsConfig config = JenkinsConfig.builder()
                               .accountId(accountId)
                               .jenkinsUrl(yaml.getUrl())
                               .encryptedPassword(yaml.getPassword())
                               .encryptedToken(yaml.getToken())
                               .authMechanism(yaml.getAuthMechanism())
                               .username(yaml.getUsername())
                               .build();

    try {
      return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
    } catch (Exception e) {
      throw new HarnessException("Failed to build Setting Attribute from JenkinsConfigVerificationYaml, ", e);
    }
  }

  @Override
  public Class getYamlClass() {
    return VerificationYaml.class;
  }
}
