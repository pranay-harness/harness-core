package software.wings.yaml.directory;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX)
public class FileNode extends DirectoryNode {
  public FileNode() {
    this.setType(NodeType.FILE);
  }

  public FileNode(String accountId, String name, Class theClass) {
    super(accountId, name, theClass);
    this.setType(NodeType.FILE);
  }

  public FileNode(String accountId, String name, Class theClass, DirectoryPath directoryPath) {
    super(accountId, name, theClass, directoryPath, NodeType.FILE);
  }
}
