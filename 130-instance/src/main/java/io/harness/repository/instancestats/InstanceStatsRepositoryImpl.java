package io.harness.repository.instancestats;

import io.harness.entity.InstanceStats;
import io.harness.timescaledb.TimeScaleDBService;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class InstanceStatsRepositoryImpl implements InstanceStatsRepository {
  private TimeScaleDBService timeScaleDBService;

  public InstanceStats getLatestRecord(String accountId) {
    try (Connection dbConnection = timeScaleDBService.getDBConnection();
         PreparedStatement statement = dbConnection.prepareStatement(InstanceStatsQuery.FETCH_LATEST_RECORD.query())) {
      statement.setString(1, accountId);
      ResultSet resultSet = statement.executeQuery();
      return parseInstanceStatsRecord(resultSet);
    } catch (SQLException exception) {
      // TODO handle exception
      exception.printStackTrace();
    }
  }

  // ------------------------------- PRIVATE METHODS ------------------------------

  private InstanceStats parseInstanceStatsRecord(ResultSet resultSet) throws SQLException {
    return InstanceStats.builder()
        .accountId(resultSet.getString(InstanceStatsFields.ACCOUNTID.fieldName()))
        .envId(resultSet.getString(InstanceStatsFields.ENVID.fieldName()))
        .serviceId(resultSet.getString(InstanceStatsFields.SERVICEID.fieldName()))
        .reportedAt(resultSet.getTimestamp(InstanceStatsFields.REPORTEDAT.fieldName()))
        .build();
  }
}
