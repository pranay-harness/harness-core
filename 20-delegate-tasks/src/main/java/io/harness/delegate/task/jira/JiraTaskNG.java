package io.harness.delegate.task.jira;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class JiraTaskNG extends AbstractDelegateRunnableTask {
  @Inject JiraTaskNGHelper jiraTaskNGHelper;

  public JiraTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    JiraTaskNGParameters taskParameters = (JiraTaskNGParameters) parameters;

    return jiraTaskNGHelper.getJiraTaskResponse(taskParameters);
  }
}
