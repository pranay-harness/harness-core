/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.events;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.observers.NodeStatusUpdateObserver;
import io.harness.engine.observers.NodeUpdateInfo;
import io.harness.execution.NodeExecution;
import io.harness.observer.AsyncInformObserver;
import io.harness.timeout.TimeoutEngine;
import io.harness.timeout.trackers.events.StatusUpdateTimeoutEvent;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.List;
import java.util.concurrent.ExecutorService;

@OwnedBy(PIPELINE)
public class NodeExecutionStatusUpdateEventHandler implements AsyncInformObserver, NodeStatusUpdateObserver {
  @Inject @Named("EngineExecutorService") ExecutorService executorService;
  @Inject private TimeoutEngine timeoutEngine;

  @Override
  public void onNodeStatusUpdate(NodeUpdateInfo nodeUpdateInfo) {
    NodeExecution nodeExecution = nodeUpdateInfo.getNodeExecution();
    List<String> timeoutInstanceIds = nodeExecution.getTimeoutInstanceIds();
    if (EmptyPredicate.isNotEmpty(timeoutInstanceIds)) {
      timeoutEngine.onEvent(timeoutInstanceIds, new StatusUpdateTimeoutEvent(nodeExecution.getStatus()));
    }
  }

  @Override
  public ExecutorService getInformExecutorService() {
    return executorService;
  }
}
