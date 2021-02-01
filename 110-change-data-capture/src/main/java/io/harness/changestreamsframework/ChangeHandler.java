package io.harness.changestreamsframework;

import io.harness.timescaledb.TimeScaleDBService;

import software.wings.beans.ce.CECloudAccount;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChangeHandler {
  @Inject private TimeScaleDBService timeScaleDBService;

  public static final int MAX_RETRY_COUNT = 3;
  static final String INSERT_STATEMENT =
      "INSERT INTO AWS_TRUTH_TABLE (UUID, ACCOUNTID, AWSACCOUNTNAME, AWSACCOUNTID) VALUES (?,?,?,?) ON CONFLICT DO NOTHING;";

  static final String UPDATE_STATEMENT =
      "UPDATE AWS_TRUTH_TABLE SET ACCOUNTID = ?, AWSACCOUNTNAME = ?, AWSACCOUNTID = ? WHERE UUID = ?;";

  static final String DELETE_STATEMENT = "DELETE FROM AWS_TRUTH_TABLE WHERE UUID = '%s';";

  public void handle(String operationType, CECloudAccount ceCloudAccount, String uuid) {
    switch (operationType) {
      case "INSERT":
        log.info("Inside Insert");
        create(ceCloudAccount, uuid);
        log.info("Successfully Inserted record with uuid: {}", uuid);
        break;
      case "REPLACE":
        log.info("Inside Update");
        update(ceCloudAccount, uuid);
        log.info("Successfully Updated record with uuid: {}", uuid);
        break;
      case "DELETE":
        log.info("Inside Delete");
        delete(String.format(DELETE_STATEMENT, uuid));
        log.info("Successfully Deleted record with uuid: {}", uuid);
        break;
      default:
        log.error("Operation Type Not Supported");
    }
  }

  public boolean create(CECloudAccount ceCloudAccount, String uuid) {
    boolean successfulInsert = false;
    if (timeScaleDBService.isValid()) {
      int retryCount = 0;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement = dbConnection.prepareStatement(INSERT_STATEMENT)) {
          updateInsertStatement(statement, ceCloudAccount, uuid);
          statement.execute();
          successfulInsert = true;
        } catch (SQLException e) {
          log.error("Failed to save ceCloudAccount data,retryCount=[{}], Exception: ", retryCount, e);
          retryCount++;
        }
      }
    } else {
      log.warn("TimeScale Down");
    }
    return successfulInsert;
  }

  public boolean update(CECloudAccount ceCloudAccount, String uuid) {
    boolean successfulInsert = false;
    if (timeScaleDBService.isValid()) {
      int retryCount = 0;
      while (!successfulInsert && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             PreparedStatement statement = dbConnection.prepareStatement(UPDATE_STATEMENT)) {
          modifyUpdateStatement(statement, ceCloudAccount, uuid);
          statement.execute();
          successfulInsert = true;
        } catch (SQLException e) {
          log.error("Failed to save ceCloudAccount data,retryCount=[{}], Exception: ", retryCount, e);
          retryCount++;
        }
      }
    } else {
      log.warn("TimeScale Down");
    }
    return successfulInsert;
  }

  private void modifyUpdateStatement(PreparedStatement statement, CECloudAccount ceCloudAccount, String uuid)
      throws SQLException {
    statement.setString(1, ceCloudAccount.getAccountId());
    statement.setString(2, ceCloudAccount.getAccountName());
    statement.setString(3, ceCloudAccount.getInfraAccountId());
    statement.setString(4, uuid);
  }

  public boolean delete(String query) {
    boolean successfulDelete = false;
    if (timeScaleDBService.isValid()) {
      int retryCount = 0;
      while (!successfulDelete && retryCount < MAX_RETRY_COUNT) {
        try (Connection dbConnection = timeScaleDBService.getDBConnection();
             Statement statement = dbConnection.createStatement()) {
          statement.execute(query);
          successfulDelete = true;
        } catch (SQLException e) {
          log.error("Failed to Delete ceCloudAccount record,retryCount=[{}], Exception: ", retryCount, e);
          retryCount++;
        }
      }
    } else {
      log.warn("TimeScale Down");
    }
    return successfulDelete;
  }

  void updateInsertStatement(PreparedStatement statement, CECloudAccount ceCloudAccount, String uuid)
      throws SQLException {
    statement.setString(1, uuid);
    statement.setString(2, ceCloudAccount.getAccountId());
    statement.setString(3, ceCloudAccount.getAccountName());
    statement.setString(4, ceCloudAccount.getInfraAccountId());
  }
}
