package io.harness.perpetualtask;

import com.google.inject.Inject;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.dl.WingsPersistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class PerpetualTaskServiceImpl
    extends PerpetualTaskServiceGrpc.PerpetualTaskServiceImplBase implements PerpetualTaskService {
  private static Map<String, PerpetualTaskServiceClient> clientMap; // <clientName, client>
  private WingsPersistence persistence;

  @Inject
  PerpetualTaskServiceImpl(WingsPersistence persistence, Map<String, PerpetualTaskServiceClient> clientMap) {
    this.clientMap = clientMap;
    this.persistence = persistence;
  }

  @Override
  public String createTask(String clientName, String clientHandle, PerpetualTaskSchedule schedule) {
    String recordId = generateId(clientName, clientHandle);
    PerpetualTaskRecord record = PerpetualTaskRecord.builder()
                                     .taskId(recordId)
                                     .clientName(clientName)
                                     .clientHandle(clientHandle)
                                     .interval(schedule.getInterval())
                                     .delegateId("")
                                     .build();
    persistence.save(record);
    return recordId;
  }

  @Override
  public void deleteTask(String clientName, String clientHandle) {
    Query<PerpetualTaskRecord> query = persistence.createQuery(PerpetualTaskRecord.class)
                                           .field("clientName")
                                           .equal(clientName)
                                           .field("clientHandle")
                                           .equal(clientHandle);
    persistence.delete(query);
  }

  @Override
  public void listTaskIds(DelegateId request, StreamObserver<PerpetualTaskIdList> responseObserver) {
    PerpetualTaskIdList taskIdList =
        PerpetualTaskIdList.newBuilder().addAllTaskIdList(listTaskIds(request.getId())).build();
    responseObserver.onNext(taskIdList);
    responseObserver.onCompleted();
  }

  protected List<PerpetualTaskId> listTaskIds(String delegateId) {
    List<PerpetualTaskId> list = null;
    List<PerpetualTaskRecord> records =
        persistence.createQuery(PerpetualTaskRecord.class).field("delegateId").equal(delegateId).asList();

    List<PerpetualTaskId> taskIds = new ArrayList<>();
    for (PerpetualTaskRecord record : records) {
      taskIds.add(PerpetualTaskId.newBuilder().setId(record.getTaskId()).build());
    }
    return taskIds;
  }

  @Override
  public void getTaskContext(PerpetualTaskId request, StreamObserver<PerpetualTaskContext> responseObserver) {
    responseObserver.onNext(this.getTaskContext(request.getId()));
    responseObserver.onCompleted();
  }

  protected PerpetualTaskContext getTaskContext(String taskId) {
    PerpetualTaskRecord record = persistence.createQuery(PerpetualTaskRecord.class).field("_id").equal(taskId).get();

    PerpetualTaskServiceClient client = clientMap.get(record.getClientName());
    PerpetualTaskParams params = client.getTaskParams(record.getClientHandle());

    PerpetualTaskSchedule schedule =
        PerpetualTaskSchedule.newBuilder().setInterval(record.getInterval()).setTimeout(record.getTimeout()).build();

    return PerpetualTaskContext.newBuilder().setTaskParams(params).setTaskSchedule(schedule).build();
  }

  private String generateId(String clientName, String id) {
    // TODO: validate if the pair of clientName and clientHandle already exists
    return UUID.randomUUID().toString();
  }
}
