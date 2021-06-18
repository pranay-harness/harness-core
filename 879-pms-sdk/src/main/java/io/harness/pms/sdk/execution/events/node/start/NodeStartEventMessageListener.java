package io.harness.pms.sdk.execution.events.node.start;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.pms.contracts.execution.start.NodeStartEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;
import io.harness.pms.sdk.core.execution.events.node.start.NodeStartEventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Map;

@Singleton
public class NodeStartEventMessageListener extends PmsAbstractMessageListener<NodeStartEvent> {
  private final NodeStartEventHandler nodeStartEventHandler;

  @Inject
  public NodeStartEventMessageListener(
      @Named(SDK_SERVICE_NAME) String serviceName, NodeStartEventHandler nodeStartEventHandler) {
    super(serviceName, NodeStartEvent.class);
    this.nodeStartEventHandler = nodeStartEventHandler;
  }

  @Override
  public boolean processMessage(NodeStartEvent event, Map<String, String> metadataMap, Long timestamp) {
    return nodeStartEventHandler.handleEvent(event, metadataMap, timestamp);
  }
}
