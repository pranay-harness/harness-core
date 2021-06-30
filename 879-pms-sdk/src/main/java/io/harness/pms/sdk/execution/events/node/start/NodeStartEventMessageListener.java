package io.harness.pms.sdk.execution.events.node.start;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.pms.contracts.execution.start.NodeStartEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;
import io.harness.pms.sdk.core.execution.events.node.start.NodeStartEventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class NodeStartEventMessageListener extends PmsAbstractMessageListener<NodeStartEvent, NodeStartEventHandler> {
  @Inject
  public NodeStartEventMessageListener(
      @Named(SDK_SERVICE_NAME) String serviceName, NodeStartEventHandler nodeStartEventHandler) {
    super(serviceName, NodeStartEvent.class, nodeStartEventHandler);
  }
}
