package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitService;

import java.util.ArrayList;
import java.util.List;

public class FolderNode extends DirectoryNode {
  private boolean defaultToClosed;
  private List<DirectoryNode> children = new ArrayList<>();
  private String appId;

  public FolderNode() {
    this.setType(NodeType.FOLDER);
    this.setRestName("folders");
  }

  public FolderNode(String accountId, String name, Class theClass) {
    super(accountId, name, theClass);
    this.setType(NodeType.FOLDER);
    this.setRestName("folders");
  }

  public FolderNode(
      String accountId, String name, Class theClass, DirectoryPath directoryPath, YamlGitService yamlGitSyncService) {
    super(accountId, name, theClass, directoryPath, yamlGitSyncService, NodeType.FOLDER);
    this.setRestName("folders");
  }

  public FolderNode(String accountId, String name, Class theClass, DirectoryPath directoryPath, String appId,
      YamlGitService yamlGitSyncService) {
    super(accountId, name, theClass, directoryPath, yamlGitSyncService, NodeType.FOLDER);
    this.appId = appId;
    this.setRestName("folders");
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

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }
}
