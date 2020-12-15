package io.harness.delegate.task;

import io.harness.delegate.beans.*;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.notification.service.PagerDutySenderImpl;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class PagerDutySenderDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private PagerDutySenderImpl pagerDutySender;

  public PagerDutySenderDelegateTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    PagerDutyTaskParams pagerDutyTaskParams = (PagerDutyTaskParams) parameters;
    try {
      boolean sent = pagerDutySender.send(pagerDutyTaskParams.getPagerDutyKeys(), pagerDutyTaskParams.getPayload(),
          pagerDutyTaskParams.getLinks(), pagerDutyTaskParams.getNotificationId());
      return NotificationTaskResponse.builder().sent(sent).build();
    } catch (Exception e) {
      return NotificationTaskResponse.builder().sent(false).errorMessage(e.getMessage()).build();
    }
  }
}
