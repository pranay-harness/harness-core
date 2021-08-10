package io.harness.delegate.app;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.grpc.DelegateServiceClassicGrpcClientModule;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.DEL)
public class DelegateServiceModule extends AbstractModule {
  private final DelegateServiceConfig config;

  /**
   * Delegate Service App Config.
   */
  public DelegateServiceModule(DelegateServiceConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    install(new DelegateServiceClassicGrpcServerModule(config));
    install(new DelegateServiceClassicGrpcClientModule(config.getDelegateServiceSecret(),
        config.getGrpcClientClassicConfig().getTarget(), config.getGrpcClientClassicConfig().getAuthority()));
  }
}
