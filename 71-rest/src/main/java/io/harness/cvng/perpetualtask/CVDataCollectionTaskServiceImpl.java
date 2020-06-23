package io.harness.cvng.perpetualtask;

import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;

import java.util.HashMap;
import java.util.Map;

public class CVDataCollectionTaskServiceImpl implements CVDataCollectionTaskService {
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private CVConfigService cvConfigService;
  @Override
  public String create(String accountId, String cvConfigId) {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put("accountId", accountId);
    clientParamMap.put("cvConfigId", cvConfigId);
    clientParamMap.put("connectorId", cvConfigService.get(cvConfigId).getConnectorId());
    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(clientParamMap);
    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(1))
                                         .setTimeout(Durations.fromMinutes(15))
                                         .build();
    return perpetualTaskService.createTask(
        PerpetualTaskType.DATA_COLLECTION_TASK, accountId, clientContext, schedule, false);
  }

  @Override
  public void delete(String accountId, String taskId) {
    perpetualTaskService.deleteTask(accountId, taskId);
  }
}
