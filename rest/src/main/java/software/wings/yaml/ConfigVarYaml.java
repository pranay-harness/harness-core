package software.wings.yaml;

public class ConfigVarYaml extends GenericYaml {
  @YamlSerialize public String name;
  @YamlSerialize public String value;

  public ConfigVarYaml() {}

  public ConfigVarYaml(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
