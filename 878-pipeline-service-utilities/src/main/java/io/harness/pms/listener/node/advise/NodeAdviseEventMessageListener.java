package io.harness.pms.listener.node.advise;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviseEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;
import io.harness.pms.sdk.core.execution.events.node.advise.NodeAdviseEventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class NodeAdviseEventMessageListener extends PmsAbstractMessageListener<AdviseEvent> {
  private final NodeAdviseEventHandler nodeAdviseEventHandler;

  @Inject
  public NodeAdviseEventMessageListener(
      @Named(SDK_SERVICE_NAME) String serviceName, NodeAdviseEventHandler nodeAdviseEventHandler) {
    super(serviceName, AdviseEvent.class);
    this.nodeAdviseEventHandler = nodeAdviseEventHandler;
  }

  @Override
  public boolean processMessage(AdviseEvent event, Map<String, String> metadataMap, Long timestamp) {
    return nodeAdviseEventHandler.handleEvent(event, metadataMap, timestamp);
  }
}
