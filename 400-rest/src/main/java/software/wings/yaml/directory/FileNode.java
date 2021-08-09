package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitService;

public class FileNode extends DirectoryNode {
  public FileNode() {
    this.setType(NodeType.FILE);
  }

  public FileNode(String accountId, String name, Class theClass) {
    super(accountId, name, theClass);
    this.setType(NodeType.FILE);
  }

  public FileNode(
      String accountId, String name, Class theClass, DirectoryPath directoryPath, YamlGitService yamlGitSyncService) {
    super(accountId, name, theClass, directoryPath, yamlGitSyncService, NodeType.FILE);
  }
}
