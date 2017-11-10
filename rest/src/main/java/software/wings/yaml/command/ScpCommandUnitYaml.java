package software.wings.yaml.command;

public class ScpCommandUnitYaml extends SshCommandUnitYaml {
  public String fileCategory;
  public String destinationDirectoryPath;

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
