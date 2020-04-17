package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Singleton;

import software.wings.beans.JenkinsConfig;
import software.wings.beans.JenkinsConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class JenkinsConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, JenkinsConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    JenkinsConfig jenkinsConfig = (JenkinsConfig) settingAttribute.getValue();

    Yaml yaml =
        Yaml.builder()
            .harnessApiVersion(getHarnessApiVersion())
            .type(jenkinsConfig.getType())
            .url(jenkinsConfig.getJenkinsUrl())
            .username(jenkinsConfig.getUsername())
            .password(jenkinsConfig.getEncryptedPassword() != null ? getEncryptedValue(jenkinsConfig, "password", true)
                                                                   : null)
            .token(jenkinsConfig.getEncryptedToken() != null ? getEncryptedValue(jenkinsConfig, "token", true) : null)
            .authMechanism(jenkinsConfig.getAuthMechanism())
            .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    if (isEmpty(yaml.getAuthMechanism())) {
      yaml.setAuthMechanism(JenkinsConfig.USERNAME_DEFAULT_TEXT);
    }

    JenkinsConfig config = JenkinsConfig.builder()
                               .accountId(accountId)
                               .jenkinsUrl(yaml.getUrl())
                               .encryptedPassword(yaml.getPassword())
                               .encryptedToken(yaml.getToken())
                               .authMechanism(yaml.getAuthMechanism())
                               .username(yaml.getUsername())
                               .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
