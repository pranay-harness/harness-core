package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.MonitoringSourceImportStatus;
import io.harness.cvng.core.entities.CVConfig;

import java.util.List;

public interface MonitoringSourceImportStatusCreator {
  MonitoringSourceImportStatus createMonitoringSourceImportStatus(
      List<CVConfig> cvConfigsGroupedByMonitoringSource, int totalNumberOfEnvironments);
}
