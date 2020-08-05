package software.wings.delegatetasks.validation;

import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.jira.JiraTaskParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class JiraValidation extends AbstractDelegateValidateTask {
  public JiraValidation(
      String delegateId, DelegateTaskPackage delegateTaskPackage, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTaskPackage, consumer);
  }

  @Override
  public List<String> getCriteria() {
    Object[] parameters = getParameters();

    List<String> jiraUrls = new ArrayList<>();

    if (parameters.length > 0) {
      for (Object param : parameters) {
        JiraTaskParameters parameter = (JiraTaskParameters) param;
        if (parameter.getJiraConfig() != null) {
          jiraUrls.add(parameter.getJiraConfig().getBaseUrl());
        }
      }
    }

    return jiraUrls;
  }
}
