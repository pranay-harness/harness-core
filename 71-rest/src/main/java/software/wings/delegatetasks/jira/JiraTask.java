package software.wings.delegatetasks.jira;

import com.google.inject.Inject;

import io.harness.delegate.task.protocol.ResponseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateLogService;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class JiraTask extends AbstractDelegateRunnableTask {
  @Inject private DelegateLogService delegateLogService;

  private static final Logger logger = LoggerFactory.getLogger(JiraTask.class);

  public JiraTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    JiraAction jiraAction = (JiraAction) parameters[0];

    switch (jiraAction.getActionType()) {
      case AUTH:
        break;

      case UPDATE_TICKET:
        break;

      case CREATE_TICKET:
        break;

      default:
        break;
    }

    return null;
  }
}
