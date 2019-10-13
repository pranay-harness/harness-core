package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.graphql.datafetcher.AbstractStatsDataFetcher.MAX_RETRY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.timescaledb.DBUtils;
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
 */
@Singleton
@Slf4j
public class InstanceTimeScaleProcessor {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject private DataFetcherUtils utils;

  private String insertQuery = "INSERT INTO INSTANCE (CREATEDAT, DELETEDAT, ISDELETED, INSTANCEID, ACCOUNTID, "
      + "APPID, SERVICEID, ENVID, CLOUDPROVIDERID, INSTANCETYPE, ARTIFACTID) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
  private String updateQuery =
      "UPDATE INSTANCE SET CREATEDAT = ?, DELETEDAT = ?, ISDELETED = ?, APPID = ?, SERVICEID = ?, "
      + "ENVID = ?, CLOUDPROVIDERID = ?, INSTANCETYPE = ?, ARTIFACTID = ? WHERE ACCOUNTID = ? AND INSTANCEID = ?";
  private String deleteQuery = "UPDATE INSTANCE SET DELETEDAT = ?, ISDELETED = ? WHERE INSTANCEID = ?";
  private String getQuery = "SELECT COUNT(INSTANCEID) FROM INSTANCE WHERE INSTANCEID = ?";

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
    if (!timeScaleDBService.isValid()) {
      logger.info("TimeScaleDB not found, not processing events");
      return;
    }

    try {
      handleInstanceDeletions(instanceEvent.getDeletions(), instanceEvent.getDeletionTimestamp());
      handleInstanceInsertions(instanceEvent.getInsertions());
    } catch (Exception ex) {
      logger.error("Error while processing instance event", ex);
    }
  }

  public void handleInstanceInsertions(Set<Instance> instanceSet) {
    if (isEmpty(instanceSet)) {
      return;
    }

    instanceSet.forEach(instance -> {
      int retryCount = 0;
      while (retryCount <= MAX_RETRY) {
        try (Connection connection = timeScaleDBService.getDBConnection();
             PreparedStatement preparedStmt = connection.prepareStatement(insertQuery)) {
          setValuesToPreparedStmt(preparedStmt, instance);
          preparedStmt.executeUpdate();
          break;
        } catch (SQLException e) {
          if (retryCount >= MAX_RETRY) {
            logger.error("Create instance query failed", e);
          } else {
            logger.error("Create instance query failed for iteration {}", retryCount, e);
          }
          retryCount++;
        }
      }
    });
  }

  public void createInstance(Instance instance) {
    if (instance == null) {
      return;
    }

    int retryCount = 0;
    while (retryCount <= MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement preparedStmt = connection.prepareStatement(insertQuery)) {
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

  public void updateInstance(Instance instance) {
    if (instance == null) {
      return;
    }

    int retryCount = 0;
    while (retryCount <= MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement preparedStmt = connection.prepareStatement(updateQuery)) {
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
          logger.error("Update instance query failed for accountId {}", instance.getAccountId(), e);
        } else {
          logger.error(
              "Update instance query failed for accountId {}, retry {}", instance.getAccountId(), retryCount, e);
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

    instanceIdSet.forEach(instanceId -> {
      int retryCount = 0;
      while (retryCount <= MAX_RETRY) {
        try (Connection connection = timeScaleDBService.getDBConnection();
             PreparedStatement preparedStmt = connection.prepareStatement(deleteQuery)) {
          preparedStmt.setTimestamp(1, new Timestamp(deleteTimestamp), utils.getDefaultCalendar());
          preparedStmt.setBoolean(2, true);
          preparedStmt.setString(3, instanceId);
          preparedStmt.executeUpdate();
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
    });
  }

  public boolean checkIfInstanceExists(String instanceId) {
    int retryCount = 0;
    while (retryCount <= MAX_RETRY) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement preparedStmt = connection.prepareStatement(getQuery);) {
        preparedStmt.setString(1, instanceId);
        resultSet = preparedStmt.executeQuery();
        if (resultSet.wasNull()) {
          return false;
        }
        resultSet.next();
        int count = resultSet.getInt("count");
        return count == 1;
      } catch (SQLException e) {
        if (retryCount >= MAX_RETRY) {
          logger.error("Failed to execute query=[{}],instanceId=[{}]", getQuery, instanceId, e);
        } else {
          logger.warn(
              "Failed to execute query=[{}],instanceId=[{}],retryCount=[{}]", getQuery, instanceId, retryCount, e);
        }
        retryCount++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return false;
  }
}
