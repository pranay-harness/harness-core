package software.wings.yaml.directory;

import software.wings.service.intfc.yaml.YamlGitSyncService;

import java.util.ArrayList;
import java.util.List;

public class FolderNode extends DirectoryNode {
  private boolean defaultToClosed = false;
  private List<DirectoryNode> children = new ArrayList<>();
  private String appId;

  public FolderNode() {
    super();
    this.setType("folder");
    this.setRestName("folder");
  }

  public FolderNode(String name, Class theClass) {
    super(name, theClass);
    this.setType("folder");
    this.setRestName("folder");
  }

  public FolderNode(String name, Class theClass, DirectoryPath directoryPath, YamlGitSyncService yamlGitSyncService) {
    super(name, theClass, directoryPath, yamlGitSyncService, "folder");
    this.setRestName("folders");
  }

  public FolderNode(
      String name, Class theClass, DirectoryPath directoryPath, String appId, YamlGitSyncService yamlGitSyncService) {
    super(name, theClass, directoryPath, yamlGitSyncService, "folder");
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
