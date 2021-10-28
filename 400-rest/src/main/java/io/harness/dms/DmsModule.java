package io.harness.dms;

import io.harness.grpc.dms.AccountCrudDmsRequestHandler;
import io.harness.grpc.dms.DmsRequestHandler;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class DmsModule extends AbstractModule {
  private static volatile DmsModule instance;
  private static boolean isDmsMode;

  private DmsModule() {}

  public static DmsModule getInstance(boolean isDmsEnabled) {
    if (instance == null) {
      instance = new DmsModule();
      isDmsMode = isDmsEnabled;
    }
    return instance;
  }

  @Override
  protected void configure() {
    if (isDmsMode) {
      // todo(abhinav): change to delegate when done.
      bind(DmsProxy.class).to(DmsProxyManagerModeImpl.class);
    } else {
      bind(DmsProxy.class).to(DmsProxyManagerModeImpl.class);
    }
    bindDmsRequestHandlers();
  }

  private void bindDmsRequestHandlers() {
    Multibinder<DmsRequestHandler> dmsRequestHandlerMultibinder =
        Multibinder.newSetBinder(binder(), DmsRequestHandler.class);
    dmsRequestHandlerMultibinder.addBinding().to(AccountCrudDmsRequestHandler.class);
  }
}
