/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.testing;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

public class ComponentTestsModule extends AbstractModule {
  @Override
  protected void configure() {
    MapBinder.newMapBinder(binder(), String.class, TestExecution.class);
  }
}
