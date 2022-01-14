package io.harness.dms;

import com.google.inject.AbstractModule;

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
    // todo(abhinav): install event fmwk module and observers depending on modes.
  }
}
