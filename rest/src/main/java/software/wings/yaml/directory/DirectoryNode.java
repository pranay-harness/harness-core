package software.wings.yaml.directory;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class DirectoryNode {
  private String type;
  private String name;
  @JsonIgnore private Class theClass;
  private String className;

  public DirectoryNode() {}

  public DirectoryNode(String name, Class theClass) {
    this.name = name;
    this.theClass = theClass;
    this.className = theClass.getName();
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
}
