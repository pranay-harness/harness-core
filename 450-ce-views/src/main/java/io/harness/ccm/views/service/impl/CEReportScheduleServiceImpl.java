package io.harness.ccm.views.service.impl;

import io.harness.ccm.views.dao.CEReportScheduleDao;
import io.harness.ccm.views.entities.CEReportSchedule;
import io.harness.ccm.views.service.CEReportScheduleService;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronSequenceGenerator;

@Slf4j
public class CEReportScheduleServiceImpl implements CEReportScheduleService {
  @Inject CEReportScheduleDao ceReportScheduleDao;

  @Override
  public CEReportSchedule get(String uuid, String accountId) {
    return ceReportScheduleDao.get(uuid, accountId);
  }

  @Override
  public List<CEReportSchedule> getReportSettingByView(String viewsId, String accountId) {
    return ceReportScheduleDao.getReportSettingByView(viewsId, accountId);
  }

  @Override
  public List<CEReportSchedule> getAllByAccount(String accountId) {
    return null;
  }

  @Override
  public CEReportSchedule createReportSetting(
      CronSequenceGenerator cronTrigger, String accountId, CEReportSchedule schedule) {
    CEReportSchedule entry = get(schedule.getUuid(), accountId);
    if (entry == null) {
      Date next = cronTrigger.next(new Date());
      log.info("Next Execution Time: " + next);
      schedule.setNextExecution(next);
      schedule.setAccountId(accountId);
      schedule.setEnabled(true);
      // Remove dupes from these lists
      schedule.setRecipients(new HashSet<>(Arrays.asList(schedule.getRecipients())).toArray(new String[0]));
      schedule.setViewsId(new HashSet<>(Arrays.asList(schedule.getViewsId())).toArray(new String[0]));
      return ceReportScheduleDao.save(accountId, schedule);
    }
    return null;
  }

  @Override
  public List<CEReportSchedule> update(CronSequenceGenerator cronTrigger, String accountId, CEReportSchedule schedule) {
    // validate cron expression during updates as well
    Date next = cronTrigger.next(new Date());
    log.info("Updated next Execution Time: " + next);
    schedule.setNextExecution(next);
    if (schedule.getDescription() == null) {
      schedule.setDescription("");
    }
    // Remove dupes from these lists
    schedule.setRecipients(new HashSet<>(Arrays.asList(schedule.getRecipients())).toArray(new String[0]));
    schedule.setViewsId(new HashSet<>(Arrays.asList(schedule.getViewsId())).toArray(new String[0]));
    return ceReportScheduleDao.update(accountId, schedule);
  }

  @Override
  public void deleteAllByView(String viewsId, String accountId) {
    ceReportScheduleDao.deleteAllByView(viewsId, accountId);
  }

  @Override
  public void delete(String uuid, String accountId) {
    CEReportSchedule entry = get(uuid, accountId);
    if (entry != null) {
      ceReportScheduleDao.delete(entry.getUuid(), accountId);
    }
  }

  @Override
  public List<CEReportSchedule> getAllMatchingSchedules(String accountId, Date jobTime) {
    return ceReportScheduleDao.getAllMatchingSchedules(accountId, jobTime);
  }

  @Override
  public List<CEReportSchedule> updateNextExecution(String accountId, CEReportSchedule schedule) {
    CronSequenceGenerator cronSequenceGenerator = new CronSequenceGenerator(schedule.getUserCron());
    Date next = cronSequenceGenerator.next(new Date());
    log.info("Updated next Execution Time: " + next);
    schedule.setNextExecution(next);
    return ceReportScheduleDao.updateNextExecution(accountId, schedule);
  }
}
