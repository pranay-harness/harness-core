package software.wings.yaml.command;

public class CopyConfigCommandUnitYaml extends CommandUnitYaml {
  public String destinationParentPath;

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
