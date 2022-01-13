/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
