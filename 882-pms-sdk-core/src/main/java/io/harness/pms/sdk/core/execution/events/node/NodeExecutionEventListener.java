package io.harness.pms.sdk.core.execution.events.node;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.manage.GlobalContextManager;
import io.harness.monitoring.MonitoringContext;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListenerWithObservers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
@Singleton
public class NodeExecutionEventListener extends QueueListenerWithObservers<NodeExecutionEvent> {
  @Inject private PmsGitSyncHelper pmsGitSyncHelper;

  @Inject
  public NodeExecutionEventListener(QueueConsumer<NodeExecutionEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessageInternal(NodeExecutionEvent event) {
    try (PmsGitSyncBranchContextGuard ignore1 =
             pmsGitSyncHelper.createGitSyncBranchContextGuard(event.getNodeExecution().getAmbiance(), true);
         AutoLogContext ignore2 = event.autoLogContext()) {
      GlobalContextManager.upsertGlobalContextRecord(
          MonitoringContext.builder().isMonitoringEnabled(event.isMonitoringEnabled()).build());
    }
  }
}
