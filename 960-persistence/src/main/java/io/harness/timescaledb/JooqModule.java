/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.timescaledb;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.jooq.DSLContext;
import org.jooq.ExecuteListener;

public class JooqModule extends AbstractModule {
  private static volatile JooqModule instance;

  private JooqModule() {}

  public static JooqModule getInstance() {
    if (instance == null) {
      instance = new JooqModule();
    }

    return instance;
  }

  /**
   * @param executeListener DefaultExecuteListener is a no-ops listener, use it when in doubt.
   */
  @Provides
  @Singleton
  public DSLContext getDSLContext(@Named("TimeScaleDBConfig") TimeScaleDBConfig timeScaleDBConfig,
      @Named("PSQLExecuteListener") ExecuteListener executeListener) {
    return new DSLContextService(timeScaleDBConfig, executeListener).getDefaultDSLContext();
  }
}
