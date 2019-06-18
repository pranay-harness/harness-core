package migrations.timescaledb;

import com.google.inject.Inject;

import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import migrations.TimeScaleDBMigration;
import org.apache.ibatis.jdbc.ScriptRunner;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;

@Slf4j
public class InitSchemaMigration implements TimeScaleDBMigration {
  @Inject TimeScaleDBService timeScaleDBService;

  @Override
  public boolean migrate() {
    if (timeScaleDBService.isValid()) {
      try (Connection connection = timeScaleDBService.getDBConnection()) {
        InputStream inputstream = getClass().getClassLoader().getResourceAsStream("timescaledb/seed_script.sql");
        InputStreamReader inputStreamReader = new InputStreamReader(inputstream);
        ScriptRunner scriptRunner = new ScriptRunner(connection);
        scriptRunner.setStopOnError(true);
        scriptRunner.runScript(inputStreamReader);
        return true;
      } catch (Exception e) {
        logger.error("Failed to run migration on db", e);
        return false;
      }
    } else {
      logger.info("TIMESCALEDBSERVICE NOT AVAILABLE");
      return false;
    }
  }
}
