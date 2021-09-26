package io.harness.ng.cdOverview.service;

import io.harness.pms.execution.ExecutionStatus;

import java.util.Arrays;
import java.util.List;

public class CDDashboardServiceHelper {
  public static List<String> failedStatusList =
      Arrays.asList(ExecutionStatus.FAILED.name(), ExecutionStatus.ABORTED.name(), ExecutionStatus.EXPIRED.name(),
          ExecutionStatus.IGNOREFAILED.name(), ExecutionStatus.ERRORED.name());
}
