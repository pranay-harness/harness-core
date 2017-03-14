package software.wings.beans;

/**
 * Created by rishi on 3/13/17.
 */
public enum RoleType {
  ACCOUNT_ADMIN("Account Administrator"),
  APPLICATION_ADMIN("Application Administrator"),
  PROD_SUPPORT("Production Support"),
  NON_PROD_SUPPORT("Non-production Support");

  private final String displayName;

  RoleType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getRoleName(String appName) {
    if (this == ACCOUNT_ADMIN) {
      return displayName;
    } else {
      return appName + "::" + displayName;
    }
  }
}
