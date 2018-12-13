package software.wings.delegatetasks.jira;

public enum JiraAction {
  CREATE_TICKET("Create Ticket"),
  UPDATE_TICKET("Update Ticket"),
  AUTH("Auth"),

  GET_PROJECTS(""),
  GET_FIELDS_OPTIONS(""),
  GET_STATUSES(""),

  CREATE_AND_APPROVE_TICKET("Create Ticket and Wait for Approval"),
  APPROVE_TICKET("Wait for approval"),
  DELETE_WEBHOOK("Delete Webhook");

  private String displayName;

  JiraAction(String s) {
    displayName = s;
  }
}
