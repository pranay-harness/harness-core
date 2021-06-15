package io.harness.pms.listener.node.start;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.pms.contracts.execution.start.NodeStartEvent;
import io.harness.pms.events.base.PmsAbstractBaseMessageListenerWithObservers;
import io.harness.pms.sdk.core.execution.events.node.start.NodeStartEventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class NodeStartEventMessageListener extends PmsAbstractBaseMessageListenerWithObservers<NodeStartEvent> {
  private final NodeStartEventHandler nodeStartEventHandler;

  @Inject
  public NodeStartEventMessageListener(
      @Named(SDK_SERVICE_NAME) String serviceName, NodeStartEventHandler nodeStartEventHandler) {
    super(serviceName, NodeStartEvent.class);
    this.nodeStartEventHandler = nodeStartEventHandler;
  }

  @Override
  public boolean processMessageInternal(NodeStartEvent event) {
    return nodeStartEventHandler.handleEvent(event);
  }
}
