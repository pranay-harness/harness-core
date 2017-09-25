package software.wings.yaml.directory;

public class FileNode extends DirectoryNode {
  public FileNode() {
    super();
    this.setType("file");
  }

  public FileNode(String name, Class theClass) {
    super(name, theClass);
    this.setType("file");
  }

  public FileNode(String name, Class theClass, DirectoryPath directoryPath) {
    super(name, theClass, directoryPath);
    this.setType("file");
  }
}
