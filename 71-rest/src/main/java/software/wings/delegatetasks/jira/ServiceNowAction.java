package software.wings.delegatetasks.jira;

public enum ServiceNowAction {
  CREATE("Create"),
  UPDATE("Update"),
  CHECK_APPROVAL("CheckApproval");

  private String displayName;
  ServiceNowAction(String s) {
    displayName = s;
  }
}
