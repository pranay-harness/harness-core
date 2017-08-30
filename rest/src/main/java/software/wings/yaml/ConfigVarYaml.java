package software.wings.yaml;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (o == this)
      return true;
    if (!(o instanceof ConfigVarYaml)) {
      return false;
    }
    ConfigVarYaml cvy = (ConfigVarYaml) o;
    return Objects.equals(name, cvy.name) && Objects.equals(value, cvy.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }
}
