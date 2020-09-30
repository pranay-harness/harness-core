package io.harness.ccm.views.dao;

import com.google.inject.Inject;

import io.harness.ccm.views.entities.CEReportSchedule;
import io.harness.ccm.views.entities.CEReportSchedule.CEReportScheduleKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.List;

@Slf4j
public class CEReportScheduleDao {
  @Inject private HPersistence persistence;

  public CEReportSchedule save(String accountId, CEReportSchedule schedule) {
    logger.info("Creating report schedule for viewId {} and accountId {}", schedule.getViewsId(), accountId);
    String key = persistence.save(schedule);
    Query<CEReportSchedule> query =
        persistence.createQuery(CEReportSchedule.class).field(CEReportScheduleKeys.uuid).equal(key);
    return query.get();
  }

  public CEReportSchedule get(String uuid, String accountId) {
    logger.info("Retrieving report schedule for reportId {} and accountId {}", uuid, accountId);
    Query<CEReportSchedule> query = persistence.createQuery(CEReportSchedule.class)
                                        .field(CEReportScheduleKeys.accountId)
                                        .equal(accountId)
                                        .field(CEReportScheduleKeys.uuid)
                                        .equal(uuid);
    return query.get();
  }

  public List<CEReportSchedule> getReportSettingByView(String viewsId, String accountId) {
    logger.info("Retrieving all report schedules for viewsId {} and accountId {}", viewsId, accountId);
    Query<CEReportSchedule> query = persistence.createQuery(CEReportSchedule.class)
                                        .field(CEReportScheduleKeys.accountId)
                                        .equal(accountId)
                                        .field(CEReportScheduleKeys.viewsId)
                                        .equal(viewsId);
    return query.asList();
  }

  public List<CEReportSchedule> getAllByAccount(String accountId) {
    logger.info("Retrieving all report schedules for  accountId", accountId);
    Query<CEReportSchedule> query =
        persistence.createQuery(CEReportSchedule.class).field(CEReportScheduleKeys.accountId).equal(accountId);
    return query.asList(new FindOptions());
  }

  public List<CEReportSchedule> update(String accountId, CEReportSchedule schedule) {
    logger.info("Updating report schedules for reportId {}  and accountId {}", schedule.getUuid(), accountId);
    Query query = persistence.createQuery(CEReportSchedule.class)
                      .field(CEReportScheduleKeys.accountId)
                      .equal(accountId)
                      .field(CEReportScheduleKeys.uuid)
                      .equal(schedule.getUuid());
    UpdateOperations<CEReportSchedule> updateOperations =
        persistence.createUpdateOperations(CEReportSchedule.class)
            .set(CEReportScheduleKeys.enabled, schedule.isEnabled())
            .set(CEReportScheduleKeys.userCron, schedule.getUserCron())
            .set(CEReportScheduleKeys.name, schedule.getName())
            .set(CEReportScheduleKeys.description, schedule.getDescription())
            .set(CEReportScheduleKeys.recipients, schedule.getRecipients())
            .set(CEReportScheduleKeys.nextExecution, schedule.getNextExecution());
    persistence.update(query, updateOperations);
    return query.asList(new FindOptions());
  }

  public boolean delete(String uuid, String accountId) {
    logger.info("Deleting report schedules for reportId {} and accountId {}", uuid, accountId);
    Query query = persistence.createQuery(CEReportSchedule.class)
                      .field(CEReportScheduleKeys.accountId)
                      .equal(accountId)
                      .field(CEReportScheduleKeys.uuid)
                      .equal(uuid);
    return persistence.delete(query);
  }

  public boolean deleteAllByView(String viewsId, String accountId) {
    Query query = persistence.createQuery(CEReportSchedule.class)
                      .field(CEReportScheduleKeys.accountId)
                      .equal(accountId)
                      .field(CEReportScheduleKeys.viewsId)
                      .equal(viewsId);
    return persistence.delete(query);
  }
}
