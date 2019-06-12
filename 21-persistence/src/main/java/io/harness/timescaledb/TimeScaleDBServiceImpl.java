package io.harness.timescaledb;

import com.google.common.base.Strings;
import com.google.inject.Singleton;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp.BasicDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Singleton
@Slf4j
@NoArgsConstructor
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
    try (Connection connection = DriverManager.getConnection(timeScaleDBConfig.getTimescaledbUrl(),
             timeScaleDBConfig.getTimescaledbUsername(), timeScaleDBConfig.getTimescaledbPassword());
         Statement st = connection.createStatement(); ResultSet rs = st.executeQuery("SELECT VERSION()")) {
      if (rs.next()) {
        logger.info(rs.getString(1));
        initializeTimeScaleDB(timeScaleDBConfig);
        validDB = true;
      }

    } catch (SQLException ex) {
      logger.info("No Valid TimeScaleDB found", ex);
    }
  }

  private boolean isValid(TimeScaleDBConfig timeScaleDBConfig) {
    if (timeScaleDBConfig == null || Strings.isNullOrEmpty(timeScaleDBConfig.getTimescaledbUrl())) {
      return false;
    }
    return true;
  }

  private void initializeTimeScaleDB(TimeScaleDBConfig config) throws SQLException {
    logger.info("Initializing TimeScaleDB extension");
    ds.setUrl(config.getTimescaledbUrl());
    ds.setUsername(config.getTimescaledbUsername());
    ds.setPassword(config.getTimescaledbPassword());
    ds.setMinIdle(5);
    ds.setMaxIdle(10);
    ds.setMaxOpenPreparedStatements(5);
    Statement statement = ds.getConnection().createStatement();
    statement.execute("CREATE EXTENSION IF NOT EXISTS TIMESCALEDB CASCADE;");
    statement.close();
    logger.info("Completed initializing TimeScaleDB extension");
  }

  @Override
  public boolean isValid() {
    return validDB;
  }

  @Override
  public Connection getDBConnection() {
    if (validDB) {
      try {
        return ds.getConnection();
      } catch (SQLException e) {
        logger.error("Failed to get connection", e);
      }
    }
    return null;
  }
}
