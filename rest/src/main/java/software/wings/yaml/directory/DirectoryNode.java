package software.wings.yaml.directory;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class DirectoryNode {
  private String type;
  private String name;
  @JsonIgnore private Class theClass;
  private String className;
  private String shortClassName;
  private String restName;
  private DirectoryPath directoryPath;

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

  public DirectoryNode(String name, Class theClass, DirectoryPath directoryPath) {
    this(name, theClass);
    this.directoryPath = directoryPath;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
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
