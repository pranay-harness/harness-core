package software.wings.yaml.directory;

import java.util.ArrayList;
import java.util.List;

public class FolderNode extends DirectoryNode {
  private boolean defaultToClosed = false;
  private List<DirectoryNode> children = new ArrayList<>();

  public FolderNode() {
    super();
    this.setType("folder");
  }

  public FolderNode(String name, Class theClass) {
    super(name, theClass);
    this.setType("folder");
  }

  public boolean isDefaultToClosed() {
    return defaultToClosed;
  }

  public void setDefaultToClosed(boolean defaultToClosed) {
    this.defaultToClosed = defaultToClosed;
  }

  public List<DirectoryNode> getChildren() {
    return children;
  }

  public void setChildren(List<DirectoryNode> children) {
    this.children = children;
  }

  public void addChild(DirectoryNode child) {
    this.children.add(child);
  }
}
