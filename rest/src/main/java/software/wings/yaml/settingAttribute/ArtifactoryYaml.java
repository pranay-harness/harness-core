package software.wings.yaml.settingAttribute;

import software.wings.beans.SettingAttribute;
import software.wings.beans.config.ArtifactoryConfig;

public class ArtifactoryYaml extends ArtifactServerYaml {
  public ArtifactoryYaml() {
    super();
  }

  public ArtifactoryYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    ArtifactoryConfig artifactoryConfig = (ArtifactoryConfig) settingAttribute.getValue();
    this.setUrl(artifactoryConfig.getArtifactoryUrl());
    this.setUsername(artifactoryConfig.getUsername());
    this.setPassword(artifactoryConfig.getPassword().toString());
  }
}