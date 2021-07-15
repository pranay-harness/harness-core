package io.harness.timescaledb;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
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
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl(timeScaleDBConfig.getTimescaledbUrl());

    if (!isEmpty(timeScaleDBConfig.getTimescaledbUsername())) {
      ds.setUsername(timeScaleDBConfig.getTimescaledbUsername());
    }
    if (!isEmpty(timeScaleDBConfig.getTimescaledbPassword())) {
      ds.setPassword(timeScaleDBConfig.getTimescaledbPassword());
    }

    DefaultConfiguration configuration = new DefaultConfiguration();
    configuration.set(new DataSourceConnectionProvider(ds));
    configuration.setExecuteListener(executeListener);
    configuration.set(SQLDialect.POSTGRES);

    this.defaultDSLContext = new DefaultDSLContext(configuration);
  }
}