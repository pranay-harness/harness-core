package software.wings.delegatetasks.jira;

public enum JiraAction {
  CREATE_TICKET("Create Ticket"),
  UPDATE_TICKET("Update Ticket"),
  AUTH("Auth"),

  GET_PROJECTS("Get Projects"),
  GET_FIELDS_OPTIONS("Get Field Options"),
  GET_STATUSES("Get Statuses"),
  GET_CREATE_METADATA("Get Create Metadata"),

  FETCH_ISSUE("Fetch Issue"),
  CHECK_APPROVAL("Check Jira Approval");

  private String displayName;
  JiraAction(String s) {
    displayName = s;
  }
}
