package io.harness.ccm.views.service.impl;

import com.google.inject.Inject;

import io.harness.ccm.views.dao.CEReportScheduleDao;
import io.harness.ccm.views.entities.CEReportSchedule;
import io.harness.ccm.views.service.CEReportScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronSequenceGenerator;

import java.util.Date;
import java.util.List;

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
      logger.info("Next Execution Time: " + next);
      schedule.setNextExecution(next);
      schedule.setAccountId(accountId);
      schedule.setEnabled(true);
      return ceReportScheduleDao.save(accountId, schedule);
    }
    return null;
  }

  @Override
  public List<CEReportSchedule> update(CronSequenceGenerator cronTrigger, String accountId, CEReportSchedule schedule) {
    // validate cron expression during updates as well
    Date next = cronTrigger.next(new Date());
    logger.info("Updated next Execution Time: " + next);
    schedule.setNextExecution(next);
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
}
