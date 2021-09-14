/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.instancestats;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.models.InstanceStats;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.PL)
public class InstanceStatsRepositoryImpl implements InstanceStatsRepository {
  private TimeScaleDBService timeScaleDBService;
  private static final int MAX_RETRY_COUNT = 3;

  public InstanceStats getLatestRecord(String accountId) {
    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (
          Connection dbConnection = timeScaleDBService.getDBConnection();
          PreparedStatement statement = dbConnection.prepareStatement(InstanceStatsQuery.FETCH_LATEST_RECORD.query())) {
        statement.setString(1, accountId);
        resultSet = statement.executeQuery();
        InstanceStats instanceStats = parseInstanceStatsRecord(resultSet);
        successfulOperation = true;
        return instanceStats;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return null;
  }

  // ------------------------------- PRIVATE METHODS ------------------------------

  private InstanceStats parseInstanceStatsRecord(ResultSet resultSet) throws SQLException {
    while (resultSet != null && resultSet.next()) {
      return InstanceStats.builder()
          .accountId(
              resultSet.getString(io.harness.repositories.instancestats.InstanceStatsFields.ACCOUNTID.fieldName()))
          .envId(resultSet.getString(io.harness.repositories.instancestats.InstanceStatsFields.ENVID.fieldName()))
          .serviceId(
              resultSet.getString(io.harness.repositories.instancestats.InstanceStatsFields.SERVICEID.fieldName()))
          .reportedAt(resultSet.getTimestamp(InstanceStatsFields.REPORTEDAT.fieldName()))
          .build();
    }
    return null;
  }
}
