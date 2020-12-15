package io.harness.delegate.task;

import io.harness.delegate.beans.*;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.notification.service.MailSenderImpl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class MailSenderDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private MailSenderImpl mailSender;
  @Inject private Injector injector;

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
      boolean sent = mailSender.send(mailTaskParams.getEmailIds(), mailTaskParams.getSubject(),
          mailTaskParams.getBody(), mailTaskParams.getNotificationId(), mailTaskParams.getSmtpConfig());
      return NotificationTaskResponse.builder().sent(sent).build();
    } catch (Exception e) {
      return NotificationTaskResponse.builder().sent(false).errorMessage(e.getMessage()).build();
    }
  }
}
