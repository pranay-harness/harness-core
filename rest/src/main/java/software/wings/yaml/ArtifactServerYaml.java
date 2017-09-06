package software.wings.yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.settings.SettingValue.SettingVariableTypes;

public class ArtifactServerYaml extends GenericYaml {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @YamlSerialize public String name;
  @YamlSerialize public String url;
  @YamlSerialize public String username;
  @YamlSerialize public String password;

  public ArtifactServerYaml() {}

  public ArtifactServerYaml(SettingAttribute settingAttribute) {
    this.name = settingAttribute.getName();

    String type = settingAttribute.getValue().getType();

    if (type.equals(SettingVariableTypes.JENKINS.toString())) {
      this.url = ((JenkinsConfig) settingAttribute.getValue()).getJenkinsUrl();
      this.username = ((JenkinsConfig) settingAttribute.getValue()).getUsername();
      this.password = ((JenkinsConfig) settingAttribute.getValue()).getPassword().toString();

      return;
    }

    if (type.equals(SettingVariableTypes.BAMBOO.toString())) {
      this.url = ((BambooConfig) settingAttribute.getValue()).getBambooUrl();
      this.username = ((BambooConfig) settingAttribute.getValue()).getUsername();
      this.password = ((BambooConfig) settingAttribute.getValue()).getPassword().toString();

      return;
    }

    if (type.equals(SettingVariableTypes.DOCKER.toString())) {
      this.url = ((DockerConfig) settingAttribute.getValue()).getDockerRegistryUrl();
      this.username = ((DockerConfig) settingAttribute.getValue()).getUsername();
      this.password = ((DockerConfig) settingAttribute.getValue()).getPassword().toString();

      return;
    }

    if (type.equals(SettingVariableTypes.NEXUS.toString())) {
      this.url = ((NexusConfig) settingAttribute.getValue()).getNexusUrl();
      this.username = ((NexusConfig) settingAttribute.getValue()).getUsername();
      this.password = ((NexusConfig) settingAttribute.getValue()).getPassword().toString();

      return;
    }

    if (type.equals(SettingVariableTypes.ARTIFACTORY.toString())) {
      this.url = ((ArtifactoryConfig) settingAttribute.getValue()).getArtifactoryUrl();
      this.username = ((ArtifactoryConfig) settingAttribute.getValue()).getUsername();
      this.password = ((ArtifactoryConfig) settingAttribute.getValue()).getPassword().toString();

      return;
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}