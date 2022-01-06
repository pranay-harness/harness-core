/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.timescaledb;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.timescaledb.TimeScaleDBConfig.TimeScaleDBConfigFields;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.Getter;
import org.apache.commons.dbcp.BasicDataSource;
import org.jooq.ExecuteListener;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;

@Singleton
@OwnedBy(CE)
public class DSLContextService {
  @Getter private final DefaultDSLContext defaultDSLContext;

  @Inject
  public DSLContextService(@Named("TimeScaleDBConfig") TimeScaleDBConfig timeScaleDBConfig,
      @Named("PSQLExecuteListener") ExecuteListener executeListener) {
    // config copied from io.harness.timescaledb.TimeScaleDBServiceImpl.java
    BasicDataSource ds = new BasicDataSource();
    ds.setUrl(timeScaleDBConfig.getTimescaledbUrl());
    if (!isEmpty(timeScaleDBConfig.getTimescaledbUsername())) {
      ds.setUsername(timeScaleDBConfig.getTimescaledbUsername());
    }
    if (!isEmpty(timeScaleDBConfig.getTimescaledbPassword())) {
      ds.setPassword(timeScaleDBConfig.getTimescaledbPassword());
    }

    ds.setMinIdle(0);
    ds.setMaxIdle(10);

    ds.addConnectionProperty(
        TimeScaleDBConfigFields.connectTimeout, String.valueOf(timeScaleDBConfig.getSocketTimeout()));
    ds.addConnectionProperty(
        TimeScaleDBConfigFields.socketTimeout, String.valueOf(timeScaleDBConfig.getSocketTimeout()));
    ds.addConnectionProperty(
        TimeScaleDBConfigFields.logUnclosedConnections, String.valueOf(timeScaleDBConfig.isLogUnclosedConnections()));

    DefaultConfiguration configuration = new DefaultConfiguration();
    configuration.set(new DataSourceConnectionProvider(ds));
    configuration.setExecuteListener(executeListener);
    configuration.set(SQLDialect.POSTGRES);

    this.defaultDSLContext = new DefaultDSLContext(configuration);
  }
}
