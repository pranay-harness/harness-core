package software.wings.yaml.settingAttribute;

import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;

public class JenkinsYaml extends ArtifactServerYaml {
  public JenkinsYaml() {
    super();
  }

  public JenkinsYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    JenkinsConfig jenkinsConfig = (JenkinsConfig) settingAttribute.getValue();
    this.setUrl(jenkinsConfig.getJenkinsUrl());
    this.setUsername(jenkinsConfig.getUsername());
    this.setPassword(jenkinsConfig.getPassword().toString());
  }
}