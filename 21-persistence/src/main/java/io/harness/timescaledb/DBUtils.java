package io.harness.timescaledb;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;

@UtilityClass
@Slf4j
public class DBUtils {
  public static void close(ResultSet resultSet) {
    try {
      if (resultSet != null && !resultSet.isClosed()) {
        resultSet.close();
      }
    } catch (SQLException e) {
      logger.warn("Error while closing result set", e);
    }
  }
}
