package software.wings.yaml.command;

import software.wings.yaml.YamlSerialize;

public class ScpCommandUnitYaml extends SshCommandUnitYaml {
  @YamlSerialize public String fileCategory;
  @YamlSerialize public String destinationDirectoryPath;

  public ScpCommandUnitYaml() {
    super();
  }

  public String getFileCategory() {
    return fileCategory;
  }

  public void setFileCategory(String fileCategory) {
    this.fileCategory = fileCategory;
  }

  public String getDestinationDirectoryPath() {
    return destinationDirectoryPath;
  }

  public void setDestinationDirectoryPath(String destinationDirectoryPath) {
    this.destinationDirectoryPath = destinationDirectoryPath;
  }
}
