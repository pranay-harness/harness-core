package io.harness.service.instancesyncperpetualtask;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.AccountId;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.models.constants.InstanceSyncConstants;
import io.harness.perpetualtask.PerpetualTaskClientContextDetails;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.TaskClientParams;
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
  private DelegateServiceGrpcClient delegateServiceGrpcClient;

  @Override
  public String createPerpetualTask(
      InfrastructureMappingDTO infrastructureMappingDTO, AbstractInstanceSyncHandler abstractInstanceSyncHandler) {
    PerpetualTaskId perpetualTaskId = delegateServiceGrpcClient.createPerpetualTask(
        AccountId.newBuilder().setId(infrastructureMappingDTO.getAccountIdentifier()).build(),
        abstractInstanceSyncHandler.getPerpetualTaskType(), preparePerpetualTaskSchedule(),
        preparePerpetualTaskClientContext(infrastructureMappingDTO), false,
        abstractInstanceSyncHandler.getPerpetualTaskDescription(infrastructureMappingDTO));
    return perpetualTaskId.getId();
  }

  @Override
  public void resetPerpetualTask(String accountIdentifier, String perpetualTaskId) {
    delegateServiceGrpcClient.resetPerpetualTask(AccountId.newBuilder().setId(accountIdentifier).build(),
        PerpetualTaskId.newBuilder().setId(perpetualTaskId).build(), null);
  }

  // --------------------------- PRIVATE METHODS -------------------------------

  private PerpetualTaskClientContextDetails preparePerpetualTaskClientContext(
      InfrastructureMappingDTO infrastructureMappingDTO) {
    Map<String, String> clientContextMap = new HashMap<>();
    // TODO check if more fields required to be set in perpetual task
    //  ideally infrastructure mapping id should be enough
    clientContextMap.put(InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID, infrastructureMappingDTO.getId());
    return PerpetualTaskClientContextDetails.newBuilder()
        .setTaskClientParams(TaskClientParams.newBuilder().putAllParams(clientContextMap).build())
        .build();
  }

  private PerpetualTaskSchedule preparePerpetualTaskSchedule() {
    return PerpetualTaskSchedule.newBuilder()
        .setInterval(Durations.fromMinutes(InstanceSyncConstants.INTERVAL_MINUTES))
        .setTimeout(Durations.fromSeconds(InstanceSyncConstants.TIMEOUT_SECONDS))
        .build();
  }
}
