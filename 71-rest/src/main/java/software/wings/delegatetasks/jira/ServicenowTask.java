// TODO : @swagat move this to software.wings.delegatetasks.ticketing
package software.wings.delegatetasks.jira;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.task.TaskParameters;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.servicenow.ServiceNowTaskParameters;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ServicenowTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(ServicenowTask.class);
  private static final int TIMEOUT = 10 * 1000;

  public ServicenowTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    return null;
  }

  public ResponseData run(ServiceNowTaskParameters parameters) {
    switch (parameters.getAction()) {
      default:
        return null;
    }
  }
}
