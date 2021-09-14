/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.views.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.entities.CEReportSchedule;

import java.util.Date;
import java.util.List;

@OwnedBy(HarnessTeam.CE)
public interface CEReportScheduleService {
  CEReportSchedule get(String uuid, String accountId);
  // List all report schedules for this view id
  List<CEReportSchedule> getReportSettingByView(String viewsId, String accountId);
  // List all report schedules for this account id
  List<CEReportSchedule> getAllByAccount(String accountId);
  // Create
  CEReportSchedule createReportSetting(String accountId, CEReportSchedule schedule);
  // Update
  List<CEReportSchedule> update(String accountId, CEReportSchedule schedule);
  // Delete all report schedule for this view uuid.
  void deleteAllByView(String viewsId, String accountId);
  // Delete a report schedule by its document uuid.
  void delete(String uuid, String accountId);
  // Get all matching schedules for this time
  List<CEReportSchedule> getAllMatchingSchedules(String accountId, Date reportTime);
  // Update just the next execution time
  List<CEReportSchedule> updateNextExecution(String accountId, CEReportSchedule schedule);
}
