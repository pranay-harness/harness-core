package io.harness.cvng.core.services.api;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.ProjectParams;
import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;

import java.util.List;

@OwnedBy(CV)
public interface SetupUsageEventService {
  void sendCreateEventsForMonitoredService(ProjectParams projectParams, MonitoredServiceDTO monitoredServiceDTO);

  void sendDeleteEventsForMonitoredService(ProjectParams projectParams, String identifier);

  void sendEvents(String accountId, EntityDetailProtoDTO referredByEntity, List<EntityDetailProtoDTO> referredEntities,
      EntityTypeProtoEnum referredEntityType);
}
