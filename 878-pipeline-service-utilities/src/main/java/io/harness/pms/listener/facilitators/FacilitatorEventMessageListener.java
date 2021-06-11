package io.harness.pms.listener.facilitators;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;

import io.harness.pms.contracts.facilitators.FacilitatorEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;
import io.harness.pms.sdk.core.facilitator.eventhandler.FacilitatorEventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

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
  public boolean processMessage(FacilitatorEvent event) {
    return facilitatorEventHandler.handleEvent(event);
  }
}
