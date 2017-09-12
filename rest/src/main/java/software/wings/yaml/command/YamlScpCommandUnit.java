package software.wings.yaml.command;

import software.wings.yaml.YamlSerialize;

public class YamlScpCommandUnit extends YamlCommandUnit {
  @YamlSerialize public String fileCategory;
  @YamlSerialize public String destinationDirectoryPath;

  public YamlScpCommandUnit() {
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
