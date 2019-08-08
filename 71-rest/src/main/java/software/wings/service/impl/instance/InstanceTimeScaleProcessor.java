package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.sql.Statement.EXECUTE_FAILED;
import static software.wings.graphql.datafetcher.AbstractStatsDataFetcher.MAX_RETRY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.InstanceEvent;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.graphql.datafetcher.DataFetcherUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Set;

/**
 * Both the normal instance and container instance are handled here.
 * Once it finds the deployment is of type container, it hands off the request to ContainerInstanceHelper.
 *
 * @author rktummala on 09/11/17
 */
@Singleton
@Slf4j
public class InstanceTimeScaleProcessor {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject private DataFetcherUtils utils;

  private String insertQuery =
      "INSERT INTO INSTANCE (CREATEDAT, DELETEDAT, ISDELETED, INSTANCEID, ACCOUNTID, APPID, SERVICEID, ENVID, CLOUDPROVIDERID, INSTANCETYPE, ARTIFACTID) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
  private String updateQuery =
      "UPDATE INSTANCE SET CREATEDAT = ?, DELETEDAT = ?, ISDELETED = ?, APPID = ?, SERVICEID = ?, ENVID = ?, CLOUDPROVIDERID = ?, INSTANCETYPE = ?, ARTIFACTID = ? WHERE ACCOUNTID = ? AND INSTANCEID = ?";
  private String deleteQuery = "UPDATE INSTANCE SET DELETEDAT = ?, ISDELETED = ? WHERE INSTANCEID = ?";
  private String getQuery = "SELECT INSTANCEID FROM INSTANCE WHERE INSTANCEID = ?";

  //  CREATE TABLE IF NOT EXISTS INSTANCE (
  //      CREATEDAT TIMESTAMPTZ NOT NULL,
  //      DELETEDAT TIMESTAMPTZ,
  //      ISDELETED BOOLEAN DEFAULT FALSE,
  //      INSTANCEID TEXT NOT NULL,
  //      ACCOUNTID TEXT NOT NULL,
  //      APPID TEXT,
  //      SERVICEID TEXT,
  //      ENVID TEXT,
  //      CLOUDPROVIDERID TEXT,
  //      INSTANCETYPE TEXT,
  //      ARTIFACTID TEXT
  //  );

  public void handleInstanceChanges(InstanceEvent instanceEvent) {
    String accountId = instanceEvent.getAccountId();
    try {
      handleInstanceDeletions(instanceEvent.getDeletions(), instanceEvent.getDeletionTimestamp());
      handleInstanceInsertions(accountId, instanceEvent.getInsertions());
    } catch (Exception ex) {
      logger.error("Error while processing instance event for account {}", accountId, ex);
    }
  }

  public void handleInstanceInsertions(String accountId, Set<Instance> instanceSet) {
    if (isEmpty(instanceSet)) {
      return;
    }

    int retryCount = 0;
    while (retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement preparedStmt = connection.prepareStatement(insertQuery)) {
        instanceSet.forEach(instance -> {
          try {
            setValuesToPreparedStmt(preparedStmt, instance);
            preparedStmt.addBatch();
          } catch (SQLException e) {
            logger.error("Error while setting values for instance creation for account {}", e, accountId);
          }
        });
        int[] batch = preparedStmt.executeBatch();
        printErrorsIfAny(batch, accountId);
        break;
      } catch (SQLException e) {
        if (retryCount >= MAX_RETRY) {
          logger.error("Create instance query failed for accountId {}", accountId, e);
        } else {
          logger.error("Create instance query failed for accountId {}, retry {}", accountId, retryCount, e);
        }
        retryCount++;
      }
    }
  }

  public void createInstance(Connection connection, Instance instance) {
    if (instance == null || connection == null) {
      return;
    }

    int retryCount = 0;
    while (retryCount < MAX_RETRY) {
      try (PreparedStatement preparedStmt = connection.prepareStatement(insertQuery)) {
        setValuesToPreparedStmt(preparedStmt, instance);
        preparedStmt.executeUpdate();
        break;
      } catch (SQLException e) {
        if (retryCount >= MAX_RETRY) {
          logger.error("Create instance query failed for accountId {}", instance.getAccountId(), e);
        } else {
          logger.error(
              "Create instance query failed for accountId {}, retry {}", instance.getAccountId(), retryCount, e);
        }
        retryCount++;
      }
    }
  }

  public void updateInstance(Connection connection, Instance instance) {
    if (instance == null || connection == null) {
      return;
    }

    int retryCount = 0;
    while (retryCount < MAX_RETRY) {
      try (PreparedStatement preparedStmt = connection.prepareStatement(updateQuery)) {
        preparedStmt.setTimestamp(1, new Timestamp(instance.getCreatedAt()), utils.getDefaultCalendar());
        preparedStmt.setTimestamp(
            2, instance.isDeleted() ? new Timestamp(instance.getDeletedAt()) : null, utils.getDefaultCalendar());
        preparedStmt.setBoolean(3, instance.isDeleted());
        preparedStmt.setString(4, instance.getAppId());
        preparedStmt.setString(5, instance.getServiceId());
        preparedStmt.setString(6, instance.getEnvId());
        preparedStmt.setString(7, instance.getComputeProviderId());
        preparedStmt.setString(8, instance.getInstanceType().name());
        preparedStmt.setString(9, instance.getLastArtifactId());
        preparedStmt.setString(10, instance.getAccountId());
        preparedStmt.setString(11, instance.getUuid());
        preparedStmt.executeUpdate();
        break;
      } catch (SQLException e) {
        if (retryCount >= MAX_RETRY) {
          logger.error("Create instance query failed for accountId {}", instance.getAccountId(), e);
        } else {
          logger.error(
              "Create instance query failed for accountId {}, retry {}", instance.getAccountId(), retryCount, e);
        }
        retryCount++;
      }
    }
  }

  private void setValuesToPreparedStmt(PreparedStatement preparedStmt, Instance instance) throws SQLException {
    preparedStmt.setTimestamp(1, new Timestamp(instance.getCreatedAt()), utils.getDefaultCalendar());
    preparedStmt.setTimestamp(
        2, instance.isDeleted() ? new Timestamp(instance.getDeletedAt()) : null, utils.getDefaultCalendar());
    preparedStmt.setBoolean(3, instance.isDeleted());
    preparedStmt.setString(4, instance.getUuid());
    preparedStmt.setString(5, instance.getAccountId());
    preparedStmt.setString(6, instance.getAppId());
    preparedStmt.setString(7, instance.getServiceId());
    preparedStmt.setString(8, instance.getEnvId());
    preparedStmt.setString(9, instance.getComputeProviderId());
    preparedStmt.setString(10, instance.getInstanceType().name());
    preparedStmt.setString(11, instance.getLastArtifactId());
  }

  public void handleInstanceDeletions(Set<String> instanceIdSet, long deleteTimestamp) {
    if (isEmpty(instanceIdSet)) {
      return;
    }

    int retryCount = 0;
    while (retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement preparedStmt = connection.prepareStatement(deleteQuery)) {
        instanceIdSet.forEach(instanceId -> {
          try {
            preparedStmt.setTimestamp(1, new Timestamp(deleteTimestamp), utils.getDefaultCalendar());
            preparedStmt.setBoolean(2, true);
            preparedStmt.setString(3, instanceId);
            preparedStmt.addBatch();
          } catch (SQLException e) {
            logger.error("Error while setting values to the instance delete query", e);
          }
        });
        int[] batch = preparedStmt.executeBatch();
        printErrorsIfAny(batch, null);
        break;
      } catch (SQLException e) {
        if (retryCount >= MAX_RETRY) {
          logger.error("Delete instance query failed", e);
        } else {
          logger.error("Delete instance query failed, retry {}", retryCount, e);
        }
        retryCount++;
      }
    }
  }

  private void printErrorsIfAny(int[] batch, String accountId) {
    for (int result : batch) {
      if (result == EXECUTE_FAILED) {
        if (accountId == null) {
          logger.error("Error observed while executing delete batch query");
        } else {
          logger.error("Error observed while executing delete batch query for account {}", accountId);
        }
      }
    }
  }

  public boolean checkIfInstanceExists(Connection connection, String instanceId) {
    int retryCount = 0;
    while (retryCount < MAX_RETRY) {
      try (PreparedStatement preparedStmt = connection.prepareStatement(getQuery)) {
        preparedStmt.setString(1, instanceId);
        ResultSet resultSet = preparedStmt.executeQuery();
        if (resultSet.wasNull()) {
          return false;
        }

        resultSet.next();
        return isNotEmpty(resultSet.getString("INSTANCEID"));
      } catch (SQLException e) {
        if (retryCount >= MAX_RETRY) {
          logger.error("Failed to execute query=[{}],instanceId=[{}]", getQuery, instanceId, e);
        } else {
          logger.warn("Failed to execute query=[{}],instanceId=[{}],retryCount=[{}]", getQuery, instanceId, retryCount);
        }
        retryCount++;
      }
    }
    return false;
  }
}
