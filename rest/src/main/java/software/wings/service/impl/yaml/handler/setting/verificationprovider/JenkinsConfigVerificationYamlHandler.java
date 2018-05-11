package software.wings.service.impl.yaml.handler.setting.verificationprovider;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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

    char[] decryptedPassword = null;
    char[] decryptedToken = null;
    try {
      if (isEmpty(yaml.getAuthMechanism())) {
        yaml.setAuthMechanism(Constants.USERNAME_PASSWORD_FIELD);
      }

      switch (yaml.getAuthMechanism()) {
        case Constants.USERNAME_PASSWORD_FIELD:
          if (isEmpty(yaml.getPassword())) {
            throw new HarnessException("Password cannot be empty");
          }
          decryptedPassword = isNotEmpty(yaml.getPassword()) ? secretManager.decryptYamlRef(yaml.getPassword()) : null;
          break;
        case Constants.TOKEN_FIELD:
          if (isEmpty(yaml.getToken())) {
            throw new HarnessException("Token cannot be empty");
          }
          decryptedToken = isNotEmpty(yaml.getToken()) ? secretManager.decryptYamlRef(yaml.getToken()) : null;
          break;
        default:
          throw new HarnessException("Invalid auth mechanism :" + yaml.getAuthMechanism());
      }

    } catch (Exception e) {
      throw new HarnessException(e);
    }

    JenkinsConfig config = JenkinsConfig.builder()
                               .accountId(accountId)
                               .jenkinsUrl(yaml.getUrl())
                               .password(decryptedPassword)
                               .encryptedPassword(yaml.getPassword())
                               .token(decryptedToken)
                               .encryptedToken(yaml.getToken())
                               .authMechanism(yaml.getAuthMechanism())
                               .username(yaml.getUsername())
                               .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return VerificationYaml.class;
  }
}
