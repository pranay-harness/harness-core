package io.harness.pms.sdk.execution.events.node.resume;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.resume.NodeResumeEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;
import io.harness.pms.sdk.core.execution.events.node.resume.NodeResumeEventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class NodeResumeEventMessageListener extends PmsAbstractMessageListener<NodeResumeEvent> {
  private final NodeResumeEventHandler nodeResumeEventHandler;

  @Inject
  public NodeResumeEventMessageListener(
      @Named(SDK_SERVICE_NAME) String serviceName, NodeResumeEventHandler nodeResumeEventHandler) {
    super(serviceName, NodeResumeEvent.class);
    this.nodeResumeEventHandler = nodeResumeEventHandler;
  }

  @Override
  public boolean processMessage(NodeResumeEvent event, Map<String, String> metadataMap, Long timestamp) {
    return nodeResumeEventHandler.handleEvent(event, metadataMap, timestamp);
  }
}
