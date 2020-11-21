package io.harness.timescaledb;

import io.harness.timescaledb.TimeScaleDBConfig.TimeScaleDBConfigFields;

import com.google.common.base.Strings;
import com.google.inject.Singleton;
import com.jayway.jsonpath.internal.Utils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp.BasicDataSource;

@Singleton
@NoArgsConstructor
@Slf4j
public class TimeScaleDBServiceImpl implements TimeScaleDBService {
  private TimeScaleDBConfig timeScaleDBConfig;
  private boolean validDB;
  private BasicDataSource ds = new BasicDataSource();

  public TimeScaleDBServiceImpl(TimeScaleDBConfig timeScaleDBConfig) {
    this.timeScaleDBConfig = timeScaleDBConfig;
    initializeDB();
  }

  private void initializeDB() {
    if (!isValid(timeScaleDBConfig)) {
      validDB = false;
      return;
    }

    Properties dbProperties = new Properties();
    dbProperties.put(TimeScaleDBConfigFields.connectTimeout, String.valueOf(timeScaleDBConfig.getConnectTimeout()));
    dbProperties.put(TimeScaleDBConfigFields.socketTimeout, String.valueOf(timeScaleDBConfig.getSocketTimeout()));
    dbProperties.put(
        TimeScaleDBConfigFields.logUnclosedConnections, String.valueOf(timeScaleDBConfig.isLogUnclosedConnections()));
    if (!Utils.isEmpty(timeScaleDBConfig.getTimescaledbUsername())) {
      dbProperties.put("user", timeScaleDBConfig.getTimescaledbUsername());
    }
    if (!Utils.isEmpty(timeScaleDBConfig.getTimescaledbPassword())) {
      dbProperties.put("password", timeScaleDBConfig.getTimescaledbPassword());
    }

    try (Connection connection = DriverManager.getConnection(timeScaleDBConfig.getTimescaledbUrl(), dbProperties);
         Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("SELECT VERSION()")) {
      if (rs.next()) {
        log.info(rs.getString(1));
        initializeTimeScaleDB(timeScaleDBConfig);
        validDB = true;
      }

    } catch (SQLException ex) {
      log.info("No Valid TimeScaleDB found", ex);
    }
  }

  @Override
  public TimeScaleDBConfig getTimeScaleDBConfig() {
    return timeScaleDBConfig;
  }

  private boolean isValid(TimeScaleDBConfig timeScaleDBConfig) {
    if (timeScaleDBConfig == null || Strings.isNullOrEmpty(timeScaleDBConfig.getTimescaledbUrl())) {
      return false;
    }
    return true;
  }

  private void initializeTimeScaleDB(TimeScaleDBConfig config) throws SQLException {
    log.info("Initializing TimeScaleDB extension");
    ds.setUrl(config.getTimescaledbUrl());
    ds.setUsername(config.getTimescaledbUsername());
    ds.setPassword(config.getTimescaledbPassword());
    ds.setMinIdle(0);
    ds.setMaxIdle(10);

    ds.addConnectionProperty(
        TimeScaleDBConfigFields.connectTimeout, String.valueOf(timeScaleDBConfig.getConnectTimeout()));
    ds.addConnectionProperty(
        TimeScaleDBConfigFields.socketTimeout, String.valueOf(timeScaleDBConfig.getSocketTimeout()));
    ds.addConnectionProperty(
        TimeScaleDBConfigFields.logUnclosedConnections, String.valueOf(timeScaleDBConfig.isLogUnclosedConnections()));
    if (!Utils.isEmpty(timeScaleDBConfig.getTimescaledbUsername())) {
      ds.addConnectionProperty("user", timeScaleDBConfig.getTimescaledbUsername());
    }
    if (!Utils.isEmpty(timeScaleDBConfig.getTimescaledbPassword())) {
      ds.addConnectionProperty("password", timeScaleDBConfig.getTimescaledbPassword());
    }

    try (Connection connection = ds.getConnection(); Statement statement = connection.createStatement()) {
      statement.execute("CREATE EXTENSION IF NOT EXISTS TIMESCALEDB CASCADE;");
      log.info("Completed initializing TimeScaleDB extension");
      statement.execute("CREATE EXTENSION IF NOT EXISTS hstore;");
      log.info("Completed initializing hstore extension");
    }
  }

  @Override
  public boolean isValid() {
    return validDB;
  }

  @Override
  public Connection getDBConnection() throws SQLException {
    if (!validDB) {
      throw new SQLException("Invalid timescale db.");
    }
    log.debug("Active connections : [{}],Idle connections : [{}]", ds.getNumActive(), ds.getNumIdle());
    return ds.getConnection();
  }
}
