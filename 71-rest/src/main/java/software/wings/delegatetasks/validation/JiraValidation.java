package software.wings.delegatetasks.validation;

import software.wings.beans.DelegateTask;
import software.wings.delegatetasks.jira.JiraAction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class JiraValidation extends AbstractDelegateValidateTask {
  public JiraValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> consumer) {
    super(delegateId, delegateTask, consumer);
  }

  @Override
  public List<String> getCriteria() {
    Object[] parameters = getParameters();

    List<String> jiraUrls = new ArrayList<>();

    if (parameters.length > 0) {
      JiraAction parameter = (JiraAction) parameters[0];
      jiraUrls.add(parameter.getJiraBaseUrl());
    }

    return jiraUrls;
  }
}
