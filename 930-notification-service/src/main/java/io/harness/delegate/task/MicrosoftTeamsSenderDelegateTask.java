package io.harness.delegate.task;

import io.harness.delegate.beans.*;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.notification.service.MicrosoftTeamsSenderImpl;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class MicrosoftTeamsSenderDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private MicrosoftTeamsSenderImpl microsoftTeamsSender;

  public MicrosoftTeamsSenderDelegateTask(DelegateTaskPackage delegateTaskPackage,
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
    MicrosoftTeamsTaskParams microsoftTeamsTaskParams = (MicrosoftTeamsTaskParams) parameters;
    try {
      boolean sent = microsoftTeamsSender.send(microsoftTeamsTaskParams.getMicrosoftTeamsWebhookUrls(),
          microsoftTeamsTaskParams.getMessage(), microsoftTeamsTaskParams.getNotificationId());
      return NotificationTaskResponse.builder().sent(sent).build();
    } catch (Exception e) {
      return NotificationTaskResponse.builder().sent(false).errorMessage(e.getMessage()).build();
    }
  }
}
