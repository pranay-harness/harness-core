/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.NotificationTaskResponse;
import io.harness.delegate.beans.PagerDutyTaskParams;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.notification.beans.NotificationProcessingResponse;
import io.harness.notification.service.senders.PagerDutySenderImpl;

import com.google.inject.Inject;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
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
      NotificationProcessingResponse processingResponse = pagerDutySender.send(pagerDutyTaskParams.getPagerDutyKeys(),
          pagerDutyTaskParams.getPayload(), pagerDutyTaskParams.getLinks(), pagerDutyTaskParams.getNotificationId());
      return NotificationTaskResponse.builder().processingResponse(processingResponse).build();
    } catch (Exception e) {
      return NotificationTaskResponse.builder()
          .processingResponse(NotificationProcessingResponse.trivialResponseWithNoRetries)
          .errorMessage(e.getMessage())
          .build();
    }
  }
}
