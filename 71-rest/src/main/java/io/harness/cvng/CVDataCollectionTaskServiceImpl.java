package io.harness.cvng;

import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;

import java.util.HashMap;
import java.util.Map;

public class CVDataCollectionTaskServiceImpl implements CVDataCollectionTaskService {
  @Inject private PerpetualTaskService perpetualTaskService;
  @Override
  public String create(String accountId, String cvConfigId) {
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put("cvConfigId", cvConfigId);
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
