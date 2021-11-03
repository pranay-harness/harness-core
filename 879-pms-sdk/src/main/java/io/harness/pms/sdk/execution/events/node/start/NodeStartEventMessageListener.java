package io.harness.pms.sdk.execution.events.node.start;

import static io.harness.pms.sdk.PmsSdkModuleUtils.CORE_EXECUTOR_NAME;
import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.start.NodeStartEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;
import io.harness.pms.sdk.core.execution.events.node.start.NodeStartEventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.kubernetes.client.Exec;
import java.util.concurrent.ExecutorService;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class NodeStartEventMessageListener extends PmsAbstractMessageListener<NodeStartEvent, NodeStartEventHandler> {
  public static NodeStartEventMessageListener getInstance(
      String serviceName, NodeStartEventHandler nodeStartEventHandler, ExecutorService executorService) {
    return new NodeStartEventMessageListener(serviceName, nodeStartEventHandler, executorService);
  }

  @Inject
  public NodeStartEventMessageListener(
      String serviceName, NodeStartEventHandler nodeStartEventHandler, ExecutorService executorService) {
    super(serviceName, NodeStartEvent.class, nodeStartEventHandler, executorService);
  }
}
