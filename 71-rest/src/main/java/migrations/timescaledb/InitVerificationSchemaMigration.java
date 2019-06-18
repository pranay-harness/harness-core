package migrations.timescaledb;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import migrations.TimeScaleDBMigration;
import org.apache.ibatis.jdbc.ScriptRunner;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class InitVerificationSchemaMigration implements TimeScaleDBMigration {
  @Inject TimeScaleDBService timeScaleDBService;

  @Override
  public boolean migrate() {
    if (timeScaleDBService.isValid()) {
      Connection connection = timeScaleDBService.getDBConnection();
      try {
        InputStream inputstream = getClass().getClassLoader().getResourceAsStream("timescaledb/seed_verification.sql");
        InputStreamReader inputStreamReader = new InputStreamReader(inputstream);
        ScriptRunner scriptRunner = new ScriptRunner(connection);
        scriptRunner.setStopOnError(true);
        scriptRunner.runScript(inputStreamReader);
        return true;
      } catch (Exception e) {
        logger.error("Failed to run migration on db", e);
        return false;
      } finally {
        closeConnection(connection);
      }
    } else {
      logger.info("TIMESCALEDBSERVICE NOT AVAILABLE");
      return false;
    }
  }

  private void closeConnection(Connection connection) throws WingsException {
    try {
      connection.close();
    } catch (SQLException e) {
      throw new WingsException(e);
    }
  }
}
