package software.wings.service.impl.yaml.handler.setting.artifactserver;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.JenkinsConfig;
import software.wings.beans.JenkinsConfigYaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@OwnedBy(CDC)
@Singleton
public class JenkinsConfigYamlHandler extends ArtifactServerYamlHandler<JenkinsConfigYaml, JenkinsConfig> {
  @Override
  public JenkinsConfigYaml toYaml(SettingAttribute settingAttribute, String appId) {
    JenkinsConfig jenkinsConfig = (JenkinsConfig) settingAttribute.getValue();

    JenkinsConfigYaml yaml =
        JenkinsConfigYaml.builder()
            .harnessApiVersion(getHarnessApiVersion())
            .type(jenkinsConfig.getType())
            .url(jenkinsConfig.getJenkinsUrl())
            .username(jenkinsConfig.getUsername())
            .password(jenkinsConfig.getEncryptedPassword() != null
                    ? getEncryptedYamlRef(jenkinsConfig.getAccountId(), jenkinsConfig.getEncryptedPassword())
                    : null)
            .token(jenkinsConfig.getEncryptedToken() != null
                    ? getEncryptedYamlRef(jenkinsConfig.getAccountId(), jenkinsConfig.getEncryptedToken())
                    : null)
            .authMechanism(jenkinsConfig.getAuthMechanism())
            .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  protected SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<JenkinsConfigYaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    JenkinsConfigYaml yaml = changeContext.getYaml();
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
    return JenkinsConfigYaml.class;
  }
}
