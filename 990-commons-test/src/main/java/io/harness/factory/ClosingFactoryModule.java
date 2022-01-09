/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.factory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClosingFactoryModule extends AbstractModule {
  private ClosingFactory closingFactory;

  public ClosingFactoryModule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Provides
  @Singleton
  public ClosingFactory closingFactory() {
    return closingFactory;
  }

  @Override
  protected void configure() {
    // Nothing to configure
  }
}
