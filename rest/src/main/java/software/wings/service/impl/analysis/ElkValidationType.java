package software.wings.service.impl.analysis;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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

  @SuppressFBWarnings("ME_ENUM_FIELD_SETTER")
  public void setName(String name) {
    this.name = name;
  }
}
