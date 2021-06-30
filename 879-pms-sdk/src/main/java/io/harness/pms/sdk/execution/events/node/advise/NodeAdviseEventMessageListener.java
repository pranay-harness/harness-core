package io.harness.pms.sdk.execution.events.node.advise;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviseEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;
import io.harness.pms.sdk.core.execution.events.node.advise.NodeAdviseEventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class NodeAdviseEventMessageListener extends PmsAbstractMessageListener<AdviseEvent, NodeAdviseEventHandler> {
  @Inject
  public NodeAdviseEventMessageListener(
      @Named(SDK_SERVICE_NAME) String serviceName, NodeAdviseEventHandler nodeAdviseEventHandler) {
    super(serviceName, AdviseEvent.class, nodeAdviseEventHandler);
  }
}
