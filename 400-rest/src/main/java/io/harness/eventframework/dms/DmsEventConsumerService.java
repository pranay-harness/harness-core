/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.eventframework.dms;

import static io.harness.eventsframework.EventsFrameworkConstants.DEFAULT_MAX_PROCESSING_TIME;
import static io.harness.eventsframework.EventsFrameworkConstants.OBSERVER_EVENT_CHANNEL;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DmsEventConsumerService implements Managed {
  @Inject private DmsRemoteObserverEventConsumer dmsRemoteObserverEventConsumer;
  private ExecutorService dmsRemoteObserverStreamConsumerService;

  @Override
  public void start() {
    dmsRemoteObserverStreamConsumerService =
        Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(OBSERVER_EVENT_CHANNEL).build());

    dmsRemoteObserverStreamConsumerService.execute(dmsRemoteObserverEventConsumer);
  }

  @Override
  public void stop() throws InterruptedException {
    dmsRemoteObserverStreamConsumerService.shutdownNow();
    dmsRemoteObserverStreamConsumerService.awaitTermination(DEFAULT_MAX_PROCESSING_TIME.getSeconds(), TimeUnit.SECONDS);
  }
}
