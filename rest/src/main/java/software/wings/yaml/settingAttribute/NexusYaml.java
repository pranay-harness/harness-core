package software.wings.yaml.settingAttribute;

import software.wings.beans.SettingAttribute;
import software.wings.beans.config.NexusConfig;

public class NexusYaml extends ArtifactServerYaml {
  public NexusYaml() {
    super();
  }

  public NexusYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    NexusConfig nexusConfig = (NexusConfig) settingAttribute.getValue();
    this.setUrl(nexusConfig.getNexusUrl());
    this.setUsername(nexusConfig.getUsername());
    this.setPassword(nexusConfig.getPassword().toString());
  }
}