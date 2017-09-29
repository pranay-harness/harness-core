package software.wings.yaml.directory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.yaml.gitSync.YamlGitSync;
import software.wings.yaml.gitSync.YamlGitSync.SyncMode;

public class DirectoryNode {
  private NodeType type;
  private String name;
  @JsonIgnore private Class theClass;
  private String className;
  private String shortClassName;
  private String restName;
  private DirectoryPath directoryPath;
  private SyncMode syncMode;
  private boolean syncEnabled;

  public DirectoryNode() {}

  public DirectoryNode(String name, Class theClass) {
    this.name = name;
    this.theClass = theClass;
    this.className = theClass.getName();

    // (simple) className is the last part of fullClassName
    String[] tokens = this.className.split("\\.");
    this.shortClassName = tokens[tokens.length - 1];

    if (this.shortClassName.equals("SettingAttribute")) {
      this.restName = "settings";
    } else if (this.shortClassName.equals("ServiceCommand")) {
      this.restName = "service-commands";
    } else if (this.shortClassName.equals("ArtifactStream")) {
      this.restName = "triggers";
    } else if (this.shortClassName.equals("Account")) {
      this.restName = "setup";
    } else {
      this.restName = this.shortClassName.toLowerCase() + "s";
    }
  }

  public DirectoryNode(
      String name, Class theClass, DirectoryPath directoryPath, YamlGitSyncService yamlGitSyncService, NodeType type) {
    this(name, theClass);
    this.directoryPath = directoryPath;
    this.type = type;

    determineSyncMode(yamlGitSyncService);
  }

  public enum NodeType {
    FOLDER("folder"),
    YAML("yaml"),
    FILE("file");

    private String displayName;

    NodeType(String displayName) {
      this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
      return displayName;
    }
  }

  private void determineSyncMode(YamlGitSyncService yamlGitSyncService) {
    // we need to check YamlGitSync by using the directoryPath as the EntityId for a folder, or the last part of the
    // path for everything else
    String path = this.directoryPath.getPath();
    String[] pathParts = path.split("/");

    if (pathParts == null || pathParts.length == 0) {
      this.syncMode = SyncMode.NONE;
    } else {
      String entityId = pathParts[pathParts.length - 1];

      if (type == NodeType.FOLDER) {
        entityId = this.directoryPath.getPath();
      }

      YamlGitSync ygs = yamlGitSyncService.get(entityId);

      if (ygs != null) {
        this.syncMode = ygs.getSyncMode();
        this.syncEnabled = ygs.isEnabled();
      } else {
        this.syncMode = SyncMode.NONE;
      }
    }
  }

  public NodeType getType() {
    return type;
  }

  public void setType(NodeType type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Class getTheClass() {
    return theClass;
  }

  public void setTheClass(Class theClass) {
    this.theClass = theClass;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getShortClassName() {
    return shortClassName;
  }

  public void setShortClassName(String shortClassName) {
    this.shortClassName = shortClassName;
  }

  public String getRestName() {
    return restName;
  }

  public void setRestName(String restName) {
    this.restName = restName;
  }

  public DirectoryPath getDirectoryPath() {
    return directoryPath;
  }

  public void setDirectoryPath(DirectoryPath directoryPath) {
    this.directoryPath = directoryPath;
  }

  public SyncMode getSyncMode() {
    return syncMode;
  }

  public void setSyncMode(SyncMode syncMode) {
    this.syncMode = syncMode;
  }

  public boolean isSyncEnabled() {
    return syncEnabled;
  }

  public void setSyncEnabled(boolean syncEnabled) {
    this.syncEnabled = syncEnabled;
  }
}
