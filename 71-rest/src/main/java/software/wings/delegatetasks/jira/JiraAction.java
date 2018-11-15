package software.wings.delegatetasks.jira;

import lombok.Getter;
import lombok.Setter;

public class JiraAction {
  public enum JiraActionType { CREATE_TICKET, UPDATE_TICKET, AUTH }

  @Getter @Setter private JiraActionType actionType;
  @Getter @Setter private String jiraBaseUrl;
}
