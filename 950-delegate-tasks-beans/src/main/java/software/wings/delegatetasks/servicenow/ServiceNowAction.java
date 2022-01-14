package software.wings.delegatetasks.servicenow;

public enum ServiceNowAction {
  CREATE("Create"),
  UPDATE("Update"),
  IMPORT_SET("Import Set");

  private String displayName;
  ServiceNowAction(String s) {
    displayName = s;
  }

  public String getDisplayName() {
    return displayName;
  }
}
