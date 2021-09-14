/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.migration;

import io.harness.migration.NGMigration;
import io.harness.timescaledb.TimeScaleDBService;

import com.google.inject.Inject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.jdbc.ScriptRunner;

@Slf4j
public class UpdateTimescaleCIPipelineExecutionSummary implements NGMigration {
  @Inject TimeScaleDBService timeScaleDBService;

  @Override
  public void migrate() {
    if (timeScaleDBService.isValid()) {
      try (Connection connection = timeScaleDBService.getDBConnection()) {
        runMigration(connection, getFileName());
      } catch (Exception e) {
        log.error("Failed to run instance rename migration on db", e);
      }
    } else {
      log.info("TIMESCALEDBSERVICE NOT AVAILABLE");
    }
  }

  private String getFileName() {
    return "timescale/modify_pipeline_execution_summary_ci_timescale.sql";
  }

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
}
