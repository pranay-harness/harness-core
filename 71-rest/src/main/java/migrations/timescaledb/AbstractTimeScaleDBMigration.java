package migrations.timescaledb;

import com.google.inject.Inject;

import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import migrations.TimeScaleDBMigration;
import org.apache.ibatis.jdbc.ScriptRunner;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

@Slf4j
public abstract class AbstractTimeScaleDBMigration implements TimeScaleDBMigration {
  @Inject TimeScaleDBService timeScaleDBService;

  private void runMigration(Connection connection, String name) {
    InputStream inputstream = getClass().getClassLoader().getResourceAsStream(name);
    if (inputstream == null) {
      logger.warn("Skipping migration {} as script not found", name);
      return;
    }
    InputStreamReader inputStreamReader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);
    ScriptRunner scriptRunner = new ScriptRunner(connection);
    scriptRunner.setStopOnError(true);
    scriptRunner.runScript(inputStreamReader);
  }

  public abstract String getFileName();

  @Override
  public boolean migrate() {
    if (timeScaleDBService.isValid()) {
      try (Connection connection = timeScaleDBService.getDBConnection()) {
        runMigration(connection, getFileName());
        return true;
      } catch (Exception e) {
        logger.error("Failed to run instance rename migration on db", e);
        return false;
      }
    } else {
      logger.info("TIMESCALEDBSERVICE NOT AVAILABLE");
      return false;
    }
  }
}
