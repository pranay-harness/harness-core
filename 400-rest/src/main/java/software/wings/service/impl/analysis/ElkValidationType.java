package software.wings.service.impl.analysis;

public enum ElkValidationType {
  PASSWORD("Password"),
  TOKEN("API Token");

  private String name;

  ElkValidationType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
