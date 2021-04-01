package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.TimeScaleDBMigration;
import io.harness.timescaledb.TimeScaleDBService;

import com.google.inject.Inject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.jdbc.ScriptRunner;

@Slf4j
@TargetModule(HarnessModule._390_DB_MIGRATION)
public abstract class AbstractTimeScaleDBMigration implements TimeScaleDBMigration {
  @Inject TimeScaleDBService timeScaleDBService;

  private void runMigration(Connection connection, String name) {
    InputStream inputstream = getClass().getClassLoader().getResourceAsStream(name);
    if (inputstream == null) {
      log.warn("Skipping migration {} as script not found", name);
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
        log.error("Failed to run instance rename migration on db", e);
        return false;
      }
    } else {
      log.info("TIMESCALEDBSERVICE NOT AVAILABLE");
      return false;
    }
  }
}
