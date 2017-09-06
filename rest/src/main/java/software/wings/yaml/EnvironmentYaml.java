package software.wings.yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Environment;

public class EnvironmentYaml extends GenericYaml {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @YamlSerialize public String name;
  @YamlSerialize public String description;
  @YamlSerialize public String environmentType;

  public EnvironmentYaml() {}

  public EnvironmentYaml(Environment environment) {
    this.name = environment.getName();
    this.description = environment.getDescription();
    this.environmentType = environment.getEnvironmentType().toString();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getEnvironmentType() {
    return environmentType;
  }

  public void setEnvironmentType(String environmentType) {
    this.environmentType = environmentType;
  }
}