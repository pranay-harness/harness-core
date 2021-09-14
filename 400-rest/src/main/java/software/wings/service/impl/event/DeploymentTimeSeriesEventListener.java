/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.event;

import io.harness.event.timeseries.processor.DeploymentEventProcessor;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import software.wings.api.DeploymentTimeSeriesEvent;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * The instance
 * @author rktummala on 08/04/19
 *
 */
@Slf4j
public class DeploymentTimeSeriesEventListener extends QueueListener<DeploymentTimeSeriesEvent> {
  @Inject private DeploymentEventProcessor deploymentEventProcessor;

  @Inject
  public DeploymentTimeSeriesEventListener(QueueConsumer<DeploymentTimeSeriesEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.QueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  public void onMessage(DeploymentTimeSeriesEvent deploymentTimeSeriesEvent) {
    try {
      deploymentEventProcessor.processEvent(deploymentTimeSeriesEvent.getTimeSeriesEventInfo());
    } catch (Exception ex) {
      log.error("Failed to process DeploymentTimeSeriesEvent : [{}]",
          deploymentTimeSeriesEvent.getTimeSeriesEventInfo().toString(), ex);
    }
  }
}
