/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
