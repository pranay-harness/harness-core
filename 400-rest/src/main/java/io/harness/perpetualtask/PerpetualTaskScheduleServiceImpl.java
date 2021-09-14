/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask;

import static io.harness.perpetualtask.PerpetualTaskScheduleConstants.PERPETUAL_TASK_SCHEDULE_FLOW;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.perpetualtask.PerpetualTaskScheduleConfig;
import io.harness.delegate.beans.perpetualtask.PerpetualTaskScheduleConfig.PerpetualTaskScheduleConfigKeys;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class PerpetualTaskScheduleServiceImpl implements PerpetualTaskScheduleService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private PerpetualTaskService perpetualTaskService;

  @Override
  public PerpetualTaskScheduleConfig save(String accountId, String perpetualTaskType, long timeIntervalInMillis) {
    int retryCount = 0;
    PerpetualTaskScheduleConfig perpetualTaskScheduleConfig = null;
    Exception ex = null;
    while (retryCount < 3) {
      try (AutoLogContext ignore1 = new AccountLogContext(accountId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR);) {
        Query<PerpetualTaskScheduleConfig> query =
            queryToGetRecordByAccountIdAndPerpetualTaskType(accountId, perpetualTaskType);
        UpdateOperations updateOperations = wingsPersistence.createUpdateOperations(PerpetualTaskScheduleConfig.class);
        updateOperations.set(PerpetualTaskScheduleConfigKeys.timeIntervalInMillis, timeIntervalInMillis);
        perpetualTaskScheduleConfig = wingsPersistence.findAndModify(
            query, updateOperations, new FindAndModifyOptions().upsert(true).returnNew(true));
        if (perpetualTaskScheduleConfig != null) {
          perpetualTaskService.updateTasksSchedule(accountId, perpetualTaskType, timeIntervalInMillis);
          return perpetualTaskScheduleConfig;
        }
      } catch (Exception exception) {
        log.error("{} : Attempt {} : Exception occured during save operation", PERPETUAL_TASK_SCHEDULE_FLOW,
            retryCount + 1, exception);
        retryCount += 1;
        ex = exception;
      }
    }
    if (perpetualTaskScheduleConfig != null) {
      log.error("{} : TimeInterval updated successfully in config, but perpetual task couldn't be updated",
          PERPETUAL_TASK_SCHEDULE_FLOW, ex);
      throw new InvalidRequestException(
          "TimeInterval updated successfully in config, but perpetual task couldn't be updated");
    }
    log.error("{} : Save operation failed after 3 retry attempts", PERPETUAL_TASK_SCHEDULE_FLOW, ex);
    throw new InvalidRequestException("Save operation failed after 3 retry attempts");
  }

  @Override
  public PerpetualTaskScheduleConfig getByAccountIdAndPerpetualTaskType(String accountId, String perpetualTaskType) {
    return queryToGetRecordByAccountIdAndPerpetualTaskType(accountId, perpetualTaskType).get();
  }

  @Override
  public boolean resetByAccountIdAndPerpetualTaskType(
      String accountId, String perpetualTaskType, long timeIntervalInMillis) {
    int retryCount = 0;
    boolean isDeleted = false;
    Exception ex = null;
    while (retryCount < 3) {
      try (AutoLogContext ignore1 = new AccountLogContext(accountId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR);) {
        Query<PerpetualTaskScheduleConfig> query =
            queryToGetRecordByAccountIdAndPerpetualTaskType(accountId, perpetualTaskType);
        isDeleted = wingsPersistence.delete(query);
        if (isDeleted) {
          perpetualTaskService.updateTasksSchedule(accountId, perpetualTaskType, timeIntervalInMillis);
          return true;
        }
      } catch (Exception exception) {
        log.error("{} : Attempt {} : Exception occured during reset operation", PERPETUAL_TASK_SCHEDULE_FLOW,
            retryCount + 1, exception);
        retryCount += 1;
        ex = exception;
      }
    }
    if (isDeleted) {
      log.error(
          "{} : record corresponding to accountId: {}, perpetualTaskType:{} and timeIntervalInMillis:{} was deleted from config but perpetual task did not reset",
          PERPETUAL_TASK_SCHEDULE_FLOW, accountId, perpetualTaskType, timeIntervalInMillis, ex);
      return false;
    }
    log.error("{} : reset operation failed after 3 retry attempts", PERPETUAL_TASK_SCHEDULE_FLOW, ex);
    return false;
  }

  private Query<PerpetualTaskScheduleConfig> queryToGetRecordByAccountIdAndPerpetualTaskType(
      String accountId, String perpetualTaskType) {
    return wingsPersistence.createQuery(PerpetualTaskScheduleConfig.class)
        .field(PerpetualTaskScheduleConfigKeys.accountId)
        .equal(accountId)
        .field(PerpetualTaskScheduleConfigKeys.perpetualTaskType)
        .equal(perpetualTaskType);
  }
}
