package io.harness.timescaledb;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.timescaledb.TimeScaleDBConfig.TimeScaleDBConfigFields;

import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.Getter;
import org.apache.commons.dbcp.BasicDataSource;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;

@Singleton
@OwnedBy(CE)
public class DSLContextService {
  @Getter private final DefaultDSLContext defaultDSLContext;

  public DSLContextService(@Named("TimeScaleDBConfig") TimeScaleDBConfig timeScaleDBConfig) {
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
    configuration.set(SQLDialect.POSTGRES);

    this.defaultDSLContext = new DefaultDSLContext(configuration);
  }
}