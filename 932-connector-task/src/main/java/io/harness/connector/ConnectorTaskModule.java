/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector;

import io.harness.connector.helper.DecryptionHelper;
import io.harness.connector.helper.DecryptionHelperViaManager;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectorTaskModule extends AbstractModule {
  private static volatile ConnectorTaskModule instance;

  private static final AtomicReference<ConnectorTaskModule> instanceRef = new AtomicReference();

  public ConnectorTaskModule() {}

  @Override
  protected void configure() {
    bind(DecryptionHelper.class).to(DecryptionHelperViaManager.class);
  }

  public static ConnectorTaskModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new ConnectorTaskModule());
    }
    return instanceRef.get();
  }
}
