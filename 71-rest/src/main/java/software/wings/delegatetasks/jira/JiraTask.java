package software.wings.delegatetasks.jira;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.delegate.task.protocol.TaskParameters;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.Issue.FluentUpdate;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.JiraExecutionData;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.JiraConfig;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.jira.JiraTaskParameters;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.ExecutionStatus;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class JiraTask extends AbstractDelegateRunnableTask {
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService logService;

  private static final Logger logger = LoggerFactory.getLogger(JiraTask.class);

  public JiraTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  public ResponseData run(JiraTaskParameters parameters) {
    JiraAction jiraAction = parameters.getJiraAction();

    switch (jiraAction) {
      case AUTH:
        break;

      case UPDATE_TICKET:
        return updateTicket(parameters);

      case CREATE_TICKET:
        return createTicket(parameters);

      default:
        break;
    }

    return null;
  }

  private ResponseData updateTicket(JiraTaskParameters parameters) {
    JiraClient jira = getJiraClient(parameters);

    Issue issue = null;
    CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.RUNNING;
    try {
      issue = jira.getIssue(parameters.getIssueId());
      FluentUpdate update = issue.update();
      if (EmptyPredicate.isNotEmpty(parameters.getSummary())) {
        update.field(Field.SUMMARY, parameters.getSummary());
      }

      if (EmptyPredicate.isNotEmpty(parameters.getSummary())) {
        update.field(Field.LABELS, parameters.getLabels());
      }

      if (EmptyPredicate.isNotEmpty(parameters.getComment())) {
        issue.addComment(parameters.getComment());
      }

      update.execute();

      if (EmptyPredicate.isNotEmpty(parameters.getStatus())) {
        issue.transition().execute(parameters.getStatus());
      }

    } catch (JiraException e) {
      commandExecutionStatus = CommandExecutionStatus.FAILURE;
      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

      String errorMessage = "Failed to update the new JIRA ticket " + parameters.getIssueId();
      logger.error(errorMessage, e);
      return JiraExecutionData.builder().executionStatus(ExecutionStatus.FAILED).errorMessage(errorMessage).build();
    }

    commandExecutionStatus = CommandExecutionStatus.SUCCESS;
    saveExecutionLog(
        parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

    return JiraExecutionData.builder()
        .executionStatus(ExecutionStatus.SUCCESS)
        .errorMessage("Updated JIRA ticket " + issue.getKey() + " at " + getIssueUrl(parameters.getJiraConfig(), issue))
        .build();
  }

  private ResponseData createTicket(JiraTaskParameters parameters) {
    JiraClient jira = getJiraClient(parameters);
    Issue issue = null;

    CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.RUNNING;
    try {
      issue = jira.createIssue(parameters.getProject(), parameters.getIssueType())
                  .field(Field.SUMMARY, parameters.getSummary())
                  .field(Field.DESCRIPTION, parameters.getDescription())
                  .field(Field.ASSIGNEE, parameters.getAssignee())
                  .field(Field.LABELS, parameters.getLabels())
                  .execute();

      if (isNotBlank(parameters.getStatus())) {
        issue.transition().execute(parameters.getStatus());
      }
      commandExecutionStatus = CommandExecutionStatus.SUCCESS;
    } catch (JiraException e) {
      logger.error("Unable to create a new JIRA ticket", e);
      commandExecutionStatus = CommandExecutionStatus.FAILURE;

      saveExecutionLog(
          parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);
      return JiraExecutionData.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage("Unable to create the new JIRA ticket " + parameters.getIssueId())
          .build();
    }

    saveExecutionLog(
        parameters, "Script execution finished with status: " + commandExecutionStatus, commandExecutionStatus);

    return JiraExecutionData.builder()
        .executionStatus(ExecutionStatus.SUCCESS)
        .jiraAction(JiraAction.CREATE_TICKET)
        .errorMessage("Created JIRA ticket " + issue.getKey() + " at " + getIssueUrl(parameters.getJiraConfig(), issue))
        .issueId(issue.getId())
        .build();
  }

  private String getIssueUrl(JiraConfig jiraConfig, Issue issue) {
    try {
      URL jiraUrl = new URL(jiraConfig.getBaseUrl());
      URL issueUrl = new URL(jiraUrl, "/browse/" + issue.getKey());

      return issueUrl.toString();
    } catch (MalformedURLException e) {
      logger.info("Incorrect url");
    }

    return null;
  }

  private void saveExecutionLog(
      JiraTaskParameters parameters, String line, CommandExecutionStatus commandExecutionStatus) {
    logService.save(parameters.getAccountId(),
        aLog()
            .withAppId(parameters.getAppId())
            .withActivityId(parameters.getActivityId())
            .withLogLevel(INFO)
            .withLogLine(line)
            .withExecutionResult(commandExecutionStatus)
            .build());
  }

  private JiraClient getJiraClient(JiraTaskParameters parameters) {
    JiraConfig jiraConfig = parameters.getJiraConfig();
    encryptionService.decrypt(jiraConfig, parameters.getEncryptionDetails());
    BasicCredentials creds = new BasicCredentials(jiraConfig.getUsername(), new String(jiraConfig.getPassword()));

    return new JiraClient(jiraConfig.getBaseUrl(), creds);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    return run((JiraTaskParameters) parameters[0]);
  }
}
