package software.wings.yaml.command;

import software.wings.yaml.YamlSerialize;

public class YamlCopyConfigCommandUnit extends YamlCommandUnit {
  @YamlSerialize public String destinationParentPath;

  public YamlCopyConfigCommandUnit() {
    super();
  }

  public String getDestinationParentPath() {
    return destinationParentPath;
  }

  public void setDestinationParentPath(String destinationParentPath) {
    this.destinationParentPath = destinationParentPath;
  }
}
