package software.wings.delegatetasks.servicenow;

public enum ServiceNowAction {
  CREATE("Create"),
  UPDATE("Update");

  private String displayName;
  ServiceNowAction(String s) {
    displayName = s;
  }
}
