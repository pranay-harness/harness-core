package io.harness.pms.sdk.execution.events.facilitators;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.pms.contracts.facilitators.FacilitatorEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;
import io.harness.pms.sdk.core.execution.events.node.facilitate.FacilitatorEventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Map;

@Singleton
public class FacilitatorEventMessageListener extends PmsAbstractMessageListener<FacilitatorEvent> {
  private final FacilitatorEventHandler facilitatorEventHandler;

  @Inject
  public FacilitatorEventMessageListener(
      @Named(SDK_SERVICE_NAME) String serviceName, FacilitatorEventHandler facilitatorEventHandler) {
    super(serviceName, FacilitatorEvent.class);
    this.facilitatorEventHandler = facilitatorEventHandler;
  }

  @Override
  public void processMessage(FacilitatorEvent event, Map<String, String> metadataMap, Long timestamp) {
    facilitatorEventHandler.handleEvent(event, metadataMap, timestamp);
  }
}
