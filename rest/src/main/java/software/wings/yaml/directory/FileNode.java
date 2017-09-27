package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitSyncService;

public class FileNode extends DirectoryNode {
  public FileNode() {
    super();
    this.setType("file");
  }

  public FileNode(String name, Class theClass) {
    super(name, theClass);
    this.setType("file");
  }

  public FileNode(String name, Class theClass, DirectoryPath directoryPath, YamlGitSyncService yamlGitSyncService) {
    super(name, theClass, directoryPath, yamlGitSyncService, "file");
  }
}
