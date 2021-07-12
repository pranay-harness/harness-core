package io.harness.service.instancesyncperpetualtask;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.infrastructuremapping.InfrastructureMappingDTO;
import io.harness.models.constants.InstanceSyncConstants;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.service.instancesynchandler.AbstractInstanceSyncHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.util.Durations;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;

@Singleton
@OwnedBy(DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class InstanceSyncPerpetualTaskServiceImpl implements InstanceSyncPerpetualTaskService {
  private PerpetualTaskService perpetualTaskService;

  @Override
  public String createPerpetualTask(
      InfrastructureMappingDTO infrastructureMappingDTO, AbstractInstanceSyncHandler abstractInstanceSyncHandler) {
    return perpetualTaskService.createTask(abstractInstanceSyncHandler.getPerpetualTaskType(),
        infrastructureMappingDTO.getAccountIdentifier(), preparePerpetualTaskClientContext(infrastructureMappingDTO),
        preparePerpetualTaskSchedule(), false,
        abstractInstanceSyncHandler.getPerpetualTaskDescription(infrastructureMappingDTO));
  }

  @Override
  public void resetPerpetualTask(String accountIdentifier, String perpetualTaskId) {
    perpetualTaskService.resetTask(accountIdentifier, perpetualTaskId, null);
  }

  // --------------------------- PRIVATE METHODS -------------------------------

  private PerpetualTaskClientContext preparePerpetualTaskClientContext(
      InfrastructureMappingDTO infrastructureMappingDTO) {
    Map<String, String> clientContextMap = new HashMap<>();
    // TODO check if more fields required to be set in perpetual task
    //  ideally infrastructure mapping id should be enough
    clientContextMap.put(InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID, infrastructureMappingDTO.getId());
    return PerpetualTaskClientContext.builder().clientParams(clientContextMap).build();
  }

  private PerpetualTaskSchedule preparePerpetualTaskSchedule() {
    return PerpetualTaskSchedule.newBuilder()
        .setInterval(Durations.fromMinutes(InstanceSyncConstants.INTERVAL_MINUTES))
        .setTimeout(Durations.fromSeconds(InstanceSyncConstants.TIMEOUT_SECONDS))
        .build();
  }
}
