/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.alert;

import com.google.inject.AbstractModule;

public class AlertModule extends AbstractModule {
  private static volatile AlertModule instance;

  public static AlertModule getInstance() {
    if (instance == null) {
      instance = new AlertModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bindAlerts();
  }

  private void bindAlerts() {
    //    MapBinder<AlertType, Class<? extends AlertData>> mapBinder = MapBinder.newMapBinder(
    //        binder(), new TypeLiteral<AlertType>() {}, new TypeLiteral<Class<? extends AlertData>>() {});
  }
}
