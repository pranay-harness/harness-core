package io.harness.event.reconciliation.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.time.Duration.ofMinutes;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.event.reconciliation.deployment.DeploymentReconRecord;
import io.harness.event.reconciliation.deployment.DeploymentReconRecord.DeploymentReconRecordKeys;
import io.harness.event.reconciliation.deployment.DetectionStatus;
import io.harness.event.reconciliation.deployment.ReconcilationAction;
import io.harness.event.reconciliation.deployment.ReconciliationStatus;
import io.harness.event.timeseries.processor.DeploymentEventProcessor;
import io.harness.event.usagemetrics.UsageMetricsEventPublisher;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.api.DeploymentTimeSeriesEvent;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.graphql.datafetcher.DataFetcherUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;

@Singleton
@Slf4j
public class DeploymentReconServiceImpl implements DeploymentReconService {
  @Inject HPersistence wingsPersistence;
  @Inject TimeScaleDBService timeScaleDBService;
  @Inject private PersistentLocker persistentLocker;
  @Inject private UsageMetricsEventPublisher usageMetricsEventPublisher;
  @Inject private DeploymentEventProcessor deploymentEventProcessor;
  @Inject private DataFetcherUtils utils;
  protected static final long COOL_DOWN_INTERVAL = 15 * 60 * 1000; /* 15 MINS COOL DOWN INTERVAL */

  private static final String CHECK_MISSING_DATA_QUERY =
      "SELECT COUNT(DISTINCT(EXECUTIONID)) FROM DEPLOYMENT WHERE ACCOUNTID=? AND ENDTIME>=? AND ENDTIME<=? AND PARENT_EXECUTION IS NULL;";

  private static final String CHECK_DUPLICATE_DATA_QUERY =
      "SELECT DISTINCT(D.EXECUTIONID) FROM DEPLOYMENT D,(SELECT COUNT(EXECUTIONID), EXECUTIONID FROM DEPLOYMENT A WHERE ACCOUNTID = ? AND ENDTIME>=? AND ENDTIME<=? GROUP BY EXECUTIONID HAVING COUNT(EXECUTIONID) > 1) AS B WHERE D.EXECUTIONID = B.EXECUTIONID;";

  private static final String DELETE_DUPLICATE = "DELETE FROM DEPLOYMENT WHERE EXECUTIONID IN (?);";

  private static final String FIND_DEPLOYMENT_IN_TSDB =
      "SELECT EXECUTIONID,ENDTIME FROM DEPLOYMENT WHERE EXECUTIONID=?";

  @Override
  public ReconciliationStatus performReconciliation(String accountId, long durationStartTs, long durationEndTs) {
    if (!timeScaleDBService.isValid()) {
      logger.info("TimeScaleDB is not valid, skipping reconciliation for accountID:[{}] in duration:[{}-{}]", accountId,
          new Date(durationStartTs), new Date(durationEndTs));
      return ReconciliationStatus.SUCCESS;
    }

    DeploymentReconRecord record = getLatestDeploymentReconRecord(accountId);
    if (record == null || shouldPerformReconciliation(record, durationEndTs)) {
      try (AcquiredLock ignore = persistentLocker.waitToAcquireLock(
               DeploymentReconRecord.class, "AccountID-" + accountId, ofMinutes(1), ofMinutes(5))) {
        record = getLatestDeploymentReconRecord(accountId);

        if (record != null && !shouldPerformReconciliation(record, durationEndTs)) {
          if (record.getReconciliationStatus() == ReconciliationStatus.IN_PROGRESS) {
            logger.info("Reconciliation is in progress, not running it again for accountID:[{}] in duration:[{}-{}]",
                accountId, new Date(durationStartTs), new Date(durationEndTs));
          } else {
            logger.info(
                "Reconciliation was performed recently at [{}], not running it again for accountID:[{}] in duration:[{}-{}]",
                accountId, new Date(durationStartTs), new Date(durationEndTs));
          }
          return ReconciliationStatus.SUCCESS;
        }

        record = DeploymentReconRecord.builder()
                     .accountId(accountId)
                     .reconciliationStatus(ReconciliationStatus.IN_PROGRESS)
                     .reconStartTs(System.currentTimeMillis())
                     .durationStartTs(durationStartTs)
                     .durationEndTs(durationEndTs)
                     .build();
        String id = wingsPersistence.save(record);
        logger.info("Inserted new deploymentReconRecord for accountId:[{}],uuid:[{}]", accountId, id);
        record = fetchRecord(id);

        boolean duplicatesDetected = false;
        boolean missingRecordsDetected = false;
        List<String> executionIDs = checkForDuplicates(accountId, durationStartTs, durationEndTs);
        if (isNotEmpty(executionIDs)) {
          duplicatesDetected = true;
          logger.warn("Duplicates detected for accountId:[{}] in duration:[{}-{}], executionIDs:[{}]", accountId,
              new Date(durationStartTs), new Date(durationEndTs), executionIDs);
          deleteDuplicates(accountId, durationStartTs, durationEndTs, executionIDs);
        }

        long primaryCount = getWFExecCountFromMongoDB(accountId, durationStartTs, durationEndTs);
        long secondaryCount = getWFExecutionCountFromTSDB(accountId, durationStartTs, durationEndTs);
        if (primaryCount > secondaryCount) {
          missingRecordsDetected = true;
          insertMissingRecords(accountId, durationStartTs, durationEndTs);
        } else if (primaryCount == secondaryCount) {
          logger.info("Everything is fine, no action required for accountID:[{}] in duration:[{}-{}]", accountId,
              new Date(durationStartTs), new Date(durationEndTs));
        } else {
          logger.error("Duplicates found again for accountID:[{}] in duration:[{}-{}]", accountId,
              new Date(durationStartTs), new Date(durationEndTs));
        }

        DetectionStatus detectionStatus;
        ReconcilationAction action;
        if (!missingRecordsDetected && !duplicatesDetected) {
          detectionStatus = DetectionStatus.SUCCESS;
          action = ReconcilationAction.NONE;
        } else if (!missingRecordsDetected && duplicatesDetected) {
          detectionStatus = DetectionStatus.DUPLICATE_DETECTED;
          action = ReconcilationAction.DUPLICATE_REMOVAL;
        } else if (missingRecordsDetected && !duplicatesDetected) {
          detectionStatus = DetectionStatus.MISSING_RECORDS_DETECTED;
          action = ReconcilationAction.ADD_MISSING_RECORDS;
        } else {
          detectionStatus = DetectionStatus.DUPLICATE_DETECTED_MISSING_RECORDS_DETECTED;
          action = ReconcilationAction.DUPLICATE_REMOVAL_ADD_MISSING_RECORDS;
        }

        UpdateOperations updateOperations = wingsPersistence.createUpdateOperations(DeploymentReconRecord.class);
        updateOperations.set(DeploymentReconRecordKeys.detectionStatus, detectionStatus);
        updateOperations.set(DeploymentReconRecordKeys.reconciliationStatus, ReconciliationStatus.SUCCESS);
        updateOperations.set(DeploymentReconRecordKeys.reconcilationAction, action);
        updateOperations.set(DeploymentReconRecordKeys.reconEndTs, System.currentTimeMillis());
        wingsPersistence.update(record, updateOperations);

      } catch (Exception e) {
        logger.error("Exception occurred while running reconciliation for accountID:[{}] in duration:[{}-{}]",
            accountId, new Date(durationStartTs), new Date(durationEndTs), e);
        if (record != null) {
          UpdateOperations updateOperations = wingsPersistence.createUpdateOperations(DeploymentReconRecord.class);
          updateOperations.set(DeploymentReconRecordKeys.reconciliationStatus, ReconciliationStatus.FAILED);
          updateOperations.set(DeploymentReconRecordKeys.reconEndTs, System.currentTimeMillis());
          wingsPersistence.update(record, updateOperations);
          return ReconciliationStatus.FAILED;
        }
      }
    } else {
      logger.info("Reconciliation task not required for accountId:[{}], durationStartTs: [{}], durationEndTs:[{}]",
          accountId, new Date(durationStartTs), new Date(durationEndTs));
    }
    return ReconciliationStatus.SUCCESS;
  }

  private void insertMissingRecords(String accountId, long durationStartTs, long durationEndTs) {
    try (HIterator<WorkflowExecution> iterator =
             new HIterator<>(wingsPersistence.createQuery(WorkflowExecution.class, excludeAuthority)
                                 .order(Sort.descending(WorkflowExecutionKeys.createdAt))
                                 .field(WorkflowExecutionKeys.accountId)
                                 .exists()
                                 .field(WorkflowExecutionKeys.startTs)
                                 .exists()
                                 .field(WorkflowExecutionKeys.endTs)
                                 .exists()
                                 .field(WorkflowExecutionKeys.endTs)
                                 .greaterThanOrEq(durationStartTs)
                                 .field(WorkflowExecutionKeys.endTs)
                                 .lessThanOrEq(durationEndTs)
                                 .project(WorkflowExecutionKeys.serviceExecutionSummaries, false)
                                 .fetch())) {
      for (WorkflowExecution workflowExecution : iterator) {
        checkAndAddIfRequired(workflowExecution);
      }
    }
  }

  private void checkAndAddIfRequired(@NotNull WorkflowExecution workflowExecution) {
    int totalTries = 0;
    boolean successfulInsert = false;
    while (totalTries <= 3 && !successfulInsert) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(FIND_DEPLOYMENT_IN_TSDB)) {
        statement.setString(1, workflowExecution.getUuid());
        resultSet = statement.executeQuery();
        if (resultSet.next()) {
          return;
        } else {
          DeploymentTimeSeriesEvent deploymentTimeSeriesEvent =
              usageMetricsEventPublisher.constructDeploymentTimeSeriesEvent(
                  workflowExecution.getAccountId(), workflowExecution);
          logger.info("ADDING MISSING RECORD for accountID:[{}], [{}]", workflowExecution.getAccountId(),
              deploymentTimeSeriesEvent.getTimeSeriesEventInfo());
          deploymentEventProcessor.processEvent(deploymentTimeSeriesEvent.getTimeSeriesEventInfo());
          successfulInsert = true;
        }

      } catch (SQLException ex) {
        totalTries++;
        logger.warn("Failed to query workflowExecution from TimescaleDB for workflowExecution:[{}], totalTries:[{}]",
            workflowExecution.getUuid(), totalTries, ex);
      } finally {
        DBUtils.close(resultSet);
      }
    }
  }

  private long getWFExecutionCountFromTSDB(String accountId, long durationStartTs, long durationEndTs) {
    int totalTries = 0;
    while (totalTries <= 3) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(CHECK_MISSING_DATA_QUERY)) {
        statement.setString(1, accountId);
        statement.setTimestamp(2, new Timestamp(durationStartTs), utils.getDefaultCalendar());
        statement.setTimestamp(3, new Timestamp(durationEndTs), utils.getDefaultCalendar());
        resultSet = statement.executeQuery();
        if (resultSet.next()) {
          return resultSet.getLong(1);
        } else {
          return 0;
        }
      } catch (SQLException ex) {
        totalTries++;
        logger.warn(
            "Failed to execute executionCount from TimeScaleDB for accountID:[{}] in duration:[{}-{}], totalTries:[{}]",
            accountId, new Date(durationStartTs), new Date(durationEndTs), totalTries, ex);
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return 0;
  }

  private void deleteDuplicates(String accountId, long durationStartTs, long durationEndTs, List<String> executionIDs) {
    int totalTries = 0;
    while (totalTries <= 3) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        String sqlStatement = DELETE_DUPLICATE.replace("?", getExecutionListAsString(executionIDs));
        statement.execute(sqlStatement);
        return;
      } catch (SQLException ex) {
        totalTries++;
        logger.warn(
            "Failed to delete duplicates for accountID:[{}] in duration:[{}-{}], executionIDs:[{}], totalTries:[{}]",
            accountId, new Date(durationStartTs), new Date(durationEndTs), executionIDs, totalTries, ex);
      }
    }
  }

  public String getExecutionListAsString(@NotNull List<String> executionIds) {
    StringBuilder builder = new StringBuilder();
    for (String s : executionIds) {
      builder = builder.length() > 0 ? builder.append(",").append("'").append(s).append("'")
                                     : builder.append("'").append(s).append("'");
    }
    return builder.toString();
  }

  private List<String> checkForDuplicates(String accountId, long durationStartTs, long durationEndTs) {
    int totalTries = 0;
    List<String> duplicates = new ArrayList<>();
    while (totalTries <= 3) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(CHECK_DUPLICATE_DATA_QUERY)) {
        statement.setString(1, accountId);
        statement.setTimestamp(2, new Timestamp(durationStartTs), utils.getDefaultCalendar());
        statement.setTimestamp(3, new Timestamp(durationEndTs), utils.getDefaultCalendar());
        resultSet = statement.executeQuery();
        while (resultSet.next()) {
          duplicates.add(resultSet.getString(1));
        }
        return duplicates;

      } catch (SQLException ex) {
        totalTries++;
        logger.warn(
            "Failed to execute executionCount from TimeScaleDB for accountID:[{}] in duration:[{}-{}], totalTries:[{}]",
            accountId, new Date(durationStartTs), new Date(durationEndTs), totalTries, ex);
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return duplicates;
  }

  private DeploymentReconRecord fetchRecord(String uuid) {
    return wingsPersistence.get(DeploymentReconRecord.class, uuid);
  }

  protected DeploymentReconRecord getLatestDeploymentReconRecord(@NotNull String accountId) {
    try (HIterator<DeploymentReconRecord> iterator =
             new HIterator<>(wingsPersistence.createQuery(DeploymentReconRecord.class)
                                 .field(DeploymentReconRecordKeys.accountId)
                                 .equal(accountId)
                                 .order(Sort.descending(DeploymentReconRecordKeys.durationEndTs))
                                 .fetch())) {
      if (!iterator.hasNext()) {
        return null;
      }
      return iterator.next();
    }
  }

  protected long getWFExecCountFromMongoDB(String accountId, long durationStartTs, long durationEndTs) {
    return wingsPersistence.createQuery(WorkflowExecution.class)
        .field(WorkflowExecutionKeys.accountId)
        .equal(accountId)
        .field(WorkflowExecutionKeys.startTs)
        .exists()
        .field(WorkflowExecutionKeys.endTs)
        .exists()
        .field(WorkflowExecutionKeys.endTs)
        .greaterThanOrEq(durationStartTs)
        .field(WorkflowExecutionKeys.endTs)
        .lessThanOrEq(durationEndTs)
        .field(WorkflowExecutionKeys.pipelineExecutionId)
        .doesNotExist()
        .field(WorkflowExecutionKeys.status)
        .in(ExecutionStatus.finalStatuses())
        .count();
  }

  protected boolean shouldPerformReconciliation(@NotNull DeploymentReconRecord record, Long durationEndTs) {
    if (record.getReconciliationStatus() == ReconciliationStatus.IN_PROGRESS) {
      /***
       * If the latest record in db is older than COOL_DOWN_INTERVAL, mark that reconciliation as failed and move on.
       * This is to prevent a bad record from blocking all further reconciliations
       */
      if (System.currentTimeMillis() - record.getDurationEndTs() > COOL_DOWN_INTERVAL) {
        logger.warn("Found an old record in progress: record: [{}] for accountID:[{}] in duration:[{}-{}]",
            record.getUuid(), record.getAccountId(), new Date(record.getDurationStartTs()),
            new Date(record.getDurationEndTs()));
        UpdateOperations updateOperations = wingsPersistence.createUpdateOperations(DeploymentReconRecord.class);
        updateOperations.set(DeploymentReconRecordKeys.reconciliationStatus, ReconciliationStatus.FAILED);
        updateOperations.set(DeploymentReconRecordKeys.reconEndTs, System.currentTimeMillis());
        wingsPersistence.update(record, updateOperations);
        return true;
      }

      /**
       * If a reconciliation is in progress, do not kick off another reconciliation.
       * This is to prevent managers from stamping on each other
       */

      return false;
    }

    /**
     * If reconciliation was run recently AND if the duration for which it was run was in the recent time interval,
     * lets not run it again.
     */

    final long currentTime = System.currentTimeMillis();
    if (((currentTime - record.getReconEndTs()) < COOL_DOWN_INTERVAL)
        && (durationEndTs < currentTime && durationEndTs > (currentTime - COOL_DOWN_INTERVAL))) {
      logger.info("Last recon for accountID:[{}] was run @ [{}], hence not rerunning it again", record.getAccountId(),
          new Date(record.getReconEndTs()));
      return false;
    }

    return true;
  }
}
