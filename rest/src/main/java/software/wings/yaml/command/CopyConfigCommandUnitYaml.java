package software.wings.yaml.command;

import software.wings.yaml.YamlSerialize;

public class CopyConfigCommandUnitYaml extends CommandUnitYaml {
  @YamlSerialize public String destinationParentPath;

  public CopyConfigCommandUnitYaml() {
    super();
  }

  public String getDestinationParentPath() {
    return destinationParentPath;
  }

  public void setDestinationParentPath(String destinationParentPath) {
    this.destinationParentPath = destinationParentPath;
  }
}
