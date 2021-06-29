package io.harness.pms.sdk.execution.events.facilitators;

import static io.harness.pms.sdk.PmsSdkModuleUtils.SDK_SERVICE_NAME;
import static io.harness.pms.sdk.execution.events.PmsSdkEventFrameworkConstants.SDK_PROCESSOR_SERVICE;

import io.harness.pms.contracts.facilitators.FacilitatorEvent;
import io.harness.pms.events.base.PmsAbstractMessageListener;
import io.harness.pms.sdk.core.execution.events.node.facilitate.FacilitatorEventHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;

@Singleton
public class FacilitatorEventMessageListener
    extends PmsAbstractMessageListener<FacilitatorEvent, FacilitatorEventHandler> {
  @Inject
  public FacilitatorEventMessageListener(
      @Named(SDK_SERVICE_NAME) String serviceName, FacilitatorEventHandler facilitatorEventHandler) {
    super(serviceName, FacilitatorEvent.class, facilitatorEventHandler);
  }
}
