package io.harness.perpetualtask;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.perpetualtask.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.dl.WingsPersistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class PerpetualTaskServiceImpl implements PerpetualTaskService {
  private static Map<String, PerpetualTaskServiceClient> clientMap; // <clientName, client>
  private WingsPersistence persistence;

  @Inject
  PerpetualTaskServiceImpl(WingsPersistence persistence, Map<String, PerpetualTaskServiceClient> clientMap) {
    this.clientMap = clientMap;
    this.persistence = persistence;
  }

  @Override
  public void createTask(String clientName, String clientHandle, PerpetualTaskSchedule schedule) {
    // TODO: validate if the pair of clientName and clientHandle already exists
    String recordId = UUID.randomUUID().toString();
    PerpetualTaskRecord record = PerpetualTaskRecord.builder()
                                     .uuid(recordId)
                                     .accountId(GLOBAL_ACCOUNT_ID)
                                     .clientName(clientName)
                                     .clientHandle(clientHandle)
                                     .timeout(schedule.getTimeout())
                                     .interval(schedule.getInterval())
                                     .delegateId("")
                                     .build();
    persistence.save(record);
  }

  @Override
  public void deleteTask(String clientName, String clientHandle) {
    Query<PerpetualTaskRecord> query = persistence.createQuery(PerpetualTaskRecord.class)
                                           .field(PerpetualTaskRecordKeys.accountId)
                                           .equal(GLOBAL_ACCOUNT_ID)
                                           .field(PerpetualTaskRecordKeys.clientName)
                                           .equal(clientName)
                                           .field(PerpetualTaskRecordKeys.clientHandle)
                                           .equal(clientHandle);
    persistence.delete(query);
  }

  public List<PerpetualTaskId> listTaskIds(String delegateId) {
    List<PerpetualTaskRecord> records = persistence.createQuery(PerpetualTaskRecord.class)
                                            .field(PerpetualTaskRecordKeys.accountId)
                                            .equal(GLOBAL_ACCOUNT_ID)
                                            .field(PerpetualTaskRecordKeys.delegateId)
                                            .equal(delegateId)
                                            .asList();

    List<PerpetualTaskId> taskIds = new ArrayList<>();
    for (PerpetualTaskRecord record : records) {
      taskIds.add(PerpetualTaskId.newBuilder().setId(record.getUuid()).build());
    }
    return taskIds;
  }

  public PerpetualTaskContext getTaskContext(String taskId) {
    PerpetualTaskRecord record =
        persistence.createQuery(PerpetualTaskRecord.class).field(PerpetualTaskRecordKeys.uuid).equal(taskId).get();

    PerpetualTaskServiceClient client = clientMap.get(record.getClientName());
    PerpetualTaskParams params = client.getTaskParams(record.getClientHandle());

    PerpetualTaskSchedule schedule =
        PerpetualTaskSchedule.newBuilder().setInterval(record.getInterval()).setTimeout(record.getTimeout()).build();

    return PerpetualTaskContext.newBuilder().setTaskParams(params).setTaskSchedule(schedule).build();
  }
}
