package io.harness.delegate.task.jira.connection;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.jira.JiraConnectionTaskParams;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.beans.connector.jira.connection.JiraTestConnectionTaskNGResponse;
import io.harness.delegate.beans.connector.jira.connection.JiraTestConnectionTaskNGResponse.JiraTestConnectionTaskNGResponseBuilder;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.jira.JiraTaskNGHelper;
import io.harness.delegate.task.jira.JiraTaskNGParameters;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import io.harness.jira.JiraAction;
import io.harness.logging.CommandExecutionStatus;
import org.apache.commons.lang3.NotImplementedException;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class JiraTestConnectionTaskNG extends AbstractDelegateRunnableTask {
  @Inject JiraTaskNGHelper jiraTaskNGHelper;

  public JiraTestConnectionTaskNG(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("This method is deprecated");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    JiraConnectionTaskParams jiraConnectionTaskParams = (JiraConnectionTaskParams) parameters;
    JiraConnectorDTO connectorDTO = jiraConnectionTaskParams.getJiraConnectorDTO();

    JiraTaskNGResponse jiraTaskNGResponse =
        jiraTaskNGHelper.getJiraTaskResponse(JiraTaskNGParameters.builder()
                                                 .jiraConnectorDTO(connectorDTO)
                                                 .jiraAction(JiraAction.AUTH)
                                                 .encryptionDetails(jiraConnectionTaskParams.getEncryptionDetails())
                                                 .build());

    JiraTestConnectionTaskNGResponseBuilder responseBuilder =
        JiraTestConnectionTaskNGResponse.builder().delegateMetaInfo(jiraTaskNGResponse.getDelegateMetaInfo());
    if (jiraTaskNGResponse.getExecutionStatus() == CommandExecutionStatus.SUCCESS) {
      responseBuilder.canConnect(true);
    } else {
      responseBuilder.canConnect(false).errorMessage(jiraTaskNGResponse.getErrorMessage());
    }
    return responseBuilder.build();
  }
}
