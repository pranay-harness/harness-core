package software.wings.delegatetasks.jira;

public enum JiraAction {
  CREATE_TICKET("Create Ticket"),
  UPDATE_TICKET("Update Ticket"),
  AUTH("Auth"),
  GET_PROJECTS(""),
  GET_FIELDS("");

  private String displayName;

  JiraAction(String s) {
    displayName = s;
  }
}
