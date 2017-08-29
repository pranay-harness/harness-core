package software.wings.yaml;

public class Config {
  @YamlSerialize private ConfigFromYaml config;

  public ConfigFromYaml getConfig() {
    return config;
  }

  public void setConfig(ConfigFromYaml config) {
    this.config = config;
  }
}
