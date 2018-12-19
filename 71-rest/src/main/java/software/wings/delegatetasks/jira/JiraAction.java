package software.wings.delegatetasks.jira;

public enum JiraAction {
  CREATE_TICKET("Create Ticket"),
  UPDATE_TICKET("Update Ticket"),
  AUTH("Auth"),

  GET_PROJECTS(""),
  GET_FIELDS_OPTIONS(""),
  GET_STATUSES(""),
  GET_CREATE_METADATA(""),

  CREATE_WEBHOOK("Create webhook"),
  DELETE_WEBHOOK("Delete Webhook");

  private String displayName;
  JiraAction(String s) {
    displayName = s;
  }
}
