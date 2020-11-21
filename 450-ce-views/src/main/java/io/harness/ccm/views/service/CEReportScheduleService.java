package io.harness.ccm.views.service;

import io.harness.ccm.views.entities.CEReportSchedule;

import java.util.Date;
import java.util.List;
import org.springframework.scheduling.support.CronSequenceGenerator;

public interface CEReportScheduleService {
  CEReportSchedule get(String uuid, String accountId);
  // List all report schedules for this view id
  List<CEReportSchedule> getReportSettingByView(String viewsId, String accountId);
  // List all report schedules for this account id
  List<CEReportSchedule> getAllByAccount(String accountId);
  // Create
  CEReportSchedule createReportSetting(CronSequenceGenerator cronTrigger, String accountId, CEReportSchedule schedule);
  // Update
  List<CEReportSchedule> update(CronSequenceGenerator cronTrigger, String accountId, CEReportSchedule schedule);
  // Delete all report schedule for this view uuid.
  void deleteAllByView(String viewsId, String accountId);
  // Delete a report schedule by its document uuid.
  void delete(String uuid, String accountId);
  // Get all matching schedules for this time
  List<CEReportSchedule> getAllMatchingSchedules(String accountId, Date reportTime);
  // Update just the next execution time
  List<CEReportSchedule> updateNextExecution(String accountId, CEReportSchedule schedule);
}
