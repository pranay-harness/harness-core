package software.wings.yaml.settingAttribute;

import software.wings.beans.DockerConfig;
import software.wings.beans.SettingAttribute;

public class DockerYaml extends ArtifactServerYaml {
  public DockerYaml() {
    super();
  }

  public DockerYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    DockerConfig dockerConfig = (DockerConfig) settingAttribute.getValue();
    this.setUrl(dockerConfig.getDockerRegistryUrl());
    this.setUsername(dockerConfig.getUsername());
    this.setPassword(dockerConfig.getPassword().toString());
  }
}