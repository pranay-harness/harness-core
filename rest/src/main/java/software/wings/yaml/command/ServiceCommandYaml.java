package software.wings.yaml.command;

import software.wings.yaml.GenericYaml;
import software.wings.yaml.YamlSerialize;

import java.util.ArrayList;
import java.util.List;

public class ServiceCommandYaml extends GenericYaml {
  @YamlSerialize public String name;
  @YamlSerialize public String commandUnitType;
  @YamlSerialize public String commandType;
  @YamlSerialize public int defaultVersion;
  @YamlSerialize public List<YamlTargetEnvironment> targetEnvironments = new ArrayList<>();
  @YamlSerialize public List<YamlCommandVersion> versions = new ArrayList<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCommandUnitType() {
    return commandUnitType;
  }

  public void setCommandUnitType(String commandUnitType) {
    this.commandUnitType = commandUnitType;
  }

  public String getCommandType() {
    return commandType;
  }

  public void setCommandType(String commandType) {
    this.commandType = commandType;
  }

  public int getDefaultVersion() {
    return defaultVersion;
  }

  public void setDefaultVersion(int defaultVersion) {
    this.defaultVersion = defaultVersion;
  }

  public List<YamlTargetEnvironment> getTargetEnvironments() {
    return targetEnvironments;
  }

  public void setTargetEnvironments(List<YamlTargetEnvironment> targetEnvironments) {
    this.targetEnvironments = targetEnvironments;
  }

  public List<YamlCommandVersion> getVersions() {
    return versions;
  }

  public void setVersions(List<YamlCommandVersion> versions) {
    this.versions = versions;
  }
}
