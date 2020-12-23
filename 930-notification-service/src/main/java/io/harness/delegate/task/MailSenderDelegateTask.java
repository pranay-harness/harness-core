package io.harness.delegate.task;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.MailTaskParams;
import io.harness.delegate.beans.NotificationTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.notification.beans.NotificationProcessingResponse;
import io.harness.notification.service.MailSenderImpl;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class MailSenderDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private MailSenderImpl mailSender;

  public MailSenderDelegateTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    MailTaskParams mailTaskParams = (MailTaskParams) parameters;
    try {
      NotificationProcessingResponse processingResponse = mailSender.send(mailTaskParams.getEmailIds(),
          mailTaskParams.getSubject(), mailTaskParams.getBody(), mailTaskParams.getNotificationId());
      return NotificationTaskResponse.builder().processingResponse(processingResponse).build();
    } catch (Exception e) {
      return NotificationTaskResponse.builder()
          .processingResponse(NotificationProcessingResponse.trivialResponseWithNoRetries)
          .errorMessage(e.getMessage())
          .build();
    }
  }
}
