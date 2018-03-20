package software.wings.service.impl.yaml.handler.setting.artifactserver;

import com.google.inject.Singleton;

import software.wings.beans.JenkinsConfig;
import software.wings.beans.JenkinsConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.io.IOException;
import java.util.List;

/**
 * @author rktummala on 11/19/17
 */
@Singleton
public class JenkinsConfigYamlHandler extends ArtifactServerYamlHandler<Yaml, JenkinsConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    JenkinsConfig jenkinsConfig = (JenkinsConfig) settingAttribute.getValue();

    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .type(jenkinsConfig.getType())
        .url(jenkinsConfig.getJenkinsUrl())
        .username(jenkinsConfig.getUsername())
        .password(getEncryptedValue(jenkinsConfig, "password", false))
        .build();
  }

  protected SettingAttribute toBean(SettingAttribute previous, ChangeContext<Yaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    char[] decryptedPassword;
    try {
      decryptedPassword = secretManager.decryptYamlRef(yaml.getPassword());
    } catch (IllegalAccessException | IOException | NullPointerException e) {
      // decryptYamlRef uses Preconditions.checkNotNull(encryptedData) library call that throws NPE
      // So catching NPE in order to propogate useful error message upwards
      throw new HarnessException("Exception while decrypting the password ref:" + yaml.getPassword());
    }

    JenkinsConfig config = JenkinsConfig.builder()
                               .accountId(accountId)
                               .jenkinsUrl(yaml.getUrl())
                               .password(decryptedPassword)
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
