package io.harness.timescaledb;

import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class DBUtils {
  public static void close(ResultSet resultSet) {
    try {
      if (resultSet != null && !resultSet.isClosed()) {
        resultSet.close();
      }
    } catch (SQLException e) {
      log.warn("Error while closing result set", e);
    }
  }
}
