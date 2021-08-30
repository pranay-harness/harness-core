package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.ChangeEventDashboardDTO;
import io.harness.cvng.core.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.types.ChangeCategory;

import java.util.List;

public interface ChangeEventService {
  Boolean register(String accountId, ChangeEventDTO changeEventDTO);
  List<ChangeEventDTO> get(ServiceEnvironmentParams serviceEnvironmentParams, long startTime, long endTime,
      List<ChangeCategory> changeCategories);
  ChangeEventDashboardDTO getChangeEventDashboard(
      ServiceEnvironmentParams serviceEnvironmentParams, long startTime, long endTime);
}
