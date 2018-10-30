package software.wings.yaml.directory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import software.wings.service.intfc.yaml.YamlGitService;

public class DirectoryNode {
  private String accountId;
  private NodeType type;
  private String name;
  @JsonIgnore private Class theClass;
  private String className;
  private String shortClassName;
  private String restName;
  private DirectoryPath directoryPath;

  public DirectoryNode() {}

  public DirectoryNode(String accountId, String name, Class theClass) {
    this.accountId = accountId;
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
    } else if (this.shortClassName.equals("ConfigFile")) {
      this.restName = "configs";
    } else if (this.shortClassName.equals("ArtifactStream")) {
      this.restName = "artifact-streams";
    } else if (this.shortClassName.equals("ContainerTask")) {
      this.restName = "container-tasks";
    } else if (this.shortClassName.equals("EcsServiceSpecification")) {
      this.restName = "ecs-service-spec";
    } else if (this.shortClassName.equals("HelmChartSpecification")) {
      this.restName = "helm-charts";
    } else if (this.shortClassName.equals("Defaults")) {
      this.restName = "defaults";
    } else if (this.shortClassName.equals("NotificationGroup")) {
      this.restName = "notification-groups";
    } else if (this.shortClassName.equals("LambdaSpecification")) {
      this.restName = "lambda-specs";
    } else if (this.shortClassName.equals("UserDataSpecification")) {
      this.restName = "user-data-specs";
    } else if (this.shortClassName.equals("Account")) {
      this.restName = "setup";
    } else {
      this.restName = this.shortClassName.toLowerCase() + "s";
    }
  }

  public DirectoryNode(String accountId, String name, Class theClass, DirectoryPath directoryPath,
      YamlGitService yamlGitSyncService, NodeType type) {
    this(accountId, name, theClass);
    this.directoryPath = directoryPath;
    this.type = type;
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

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
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
}
