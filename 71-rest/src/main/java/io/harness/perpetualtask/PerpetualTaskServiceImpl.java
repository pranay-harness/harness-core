package io.harness.perpetualtask;

import static io.harness.delegate.message.ManagerMessageConstants.UPDATE_PERPETUAL_TASK;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;

import io.grpc.Context;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import io.harness.grpc.utils.HTimestamps;
import io.harness.logging.AutoLogContext;
import io.harness.observer.Subject;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.perpetualtask.internal.PerpetualTaskRecordDao;
import io.harness.persistence.AccountLogContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.atmosphere.cpr.BroadcasterFactory;
import org.eclipse.jetty.util.ConcurrentHashSet;
import software.wings.beans.PerpetualTaskBroadcastEvent;
import software.wings.service.impl.DelegateTaskBroadcastHelper;
import software.wings.service.intfc.perpetualtask.PerpetualTaskCrudObserver;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class PerpetualTaskServiceImpl implements PerpetualTaskService {
  private Set<Pair<String, String>> broadcastAggregateSet = new ConcurrentHashSet<>();

  private final PerpetualTaskRecordDao perpetualTaskRecordDao;
  private final PerpetualTaskServiceClientRegistry clientRegistry;
  private final BroadcasterFactory broadcasterFactory;

  @Inject
  public PerpetualTaskServiceImpl(PerpetualTaskRecordDao perpetualTaskRecordDao,
      PerpetualTaskServiceClientRegistry clientRegistry, BroadcasterFactory broadcasterFactory) {
    this.perpetualTaskRecordDao = perpetualTaskRecordDao;
    this.clientRegistry = clientRegistry;
    this.broadcasterFactory = broadcasterFactory;
  }

  @Getter private Subject<PerpetualTaskCrudObserver> perpetualTaskCrudSubject = new Subject<>();

  @Override
  public void appointDelegate(String accountId, String taskId, String delegateId, long lastContextUpdated) {
    perpetualTaskRecordDao.appointDelegate(taskId, delegateId, lastContextUpdated);

    broadcastAggregateSet.add(Pair.of(accountId, delegateId));
  }

  public void broadcastToDelegate() {
    Set<Pair<String, String>> sendingHashSet;
    synchronized (broadcastAggregateSet) {
      sendingHashSet = new HashSet<>(broadcastAggregateSet);
      broadcastAggregateSet.clear();
    }
    sendingHashSet.forEach(setEntry
        -> broadcasterFactory.lookup(DelegateTaskBroadcastHelper.STREAM_DELEGATE_PATH + setEntry.getLeft(), true)
               .broadcast(PerpetualTaskBroadcastEvent.builder()
                              .eventType(UPDATE_PERPETUAL_TASK)
                              .broadcastDelegateId(setEntry.getRight())
                              .build()));
  }

  @Override
  public String createTask(String perpetualTaskType, String accountId, PerpetualTaskClientContext clientContext,
      PerpetualTaskSchedule schedule, boolean allowDuplicate) {
    try (AutoLogContext ignore0 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      if (!allowDuplicate) {
        Optional<PerpetualTaskRecord> perpetualTaskMaybe =
            perpetualTaskRecordDao.getExistingPerpetualTask(accountId, perpetualTaskType, clientContext);
        if (perpetualTaskMaybe.isPresent()) {
          PerpetualTaskRecord perpetualTaskRecord = perpetualTaskMaybe.get();
          logger.info("Perpetual task with id={} exists.", perpetualTaskRecord.getUuid());
          return perpetualTaskRecord.getUuid();
        }
      }

      PerpetualTaskRecord record = PerpetualTaskRecord.builder()
                                       .accountId(accountId)
                                       .perpetualTaskType(perpetualTaskType)
                                       .clientContext(clientContext)
                                       .timeoutMillis(Durations.toMillis(schedule.getTimeout()))
                                       .intervalSeconds(schedule.getInterval().getSeconds())
                                       .delegateId("")
                                       .state(PerpetualTaskState.TASK_UNASSIGNED.name())
                                       .build();

      perpetualTaskCrudSubject.fireInform(PerpetualTaskCrudObxxxxxxxx:onPerpetualTaskCreated);
      String taskId = perpetualTaskRecordDao.save(record);
      try (AutoLogContext ignore1 = new PerpetualTaskLogContext(taskId, OVERRIDE_ERROR)) {
        logger.info("Created a perpetual task with id={}.", taskId);
      }
      return taskId;
    }
  }

  @Override
  public boolean resetTask(String accountId, String taskId) {
    // TODO(Hitesh) -> make a callback
    logger.info("Resetting the perpetual task with id={}.", taskId);
    perpetualTaskRecordDao.setTaskState(taskId, PerpetualTaskState.TASK_UNASSIGNED.name());
    return perpetualTaskRecordDao.resetDelegateIdForTask(accountId, taskId);
  }

  @Override
  public boolean deleteTask(String accountId, String taskId) {
    try (AutoLogContext ignore0 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore1 = new PerpetualTaskLogContext(taskId, OVERRIDE_ERROR)) {
      boolean hasDeleted = perpetualTaskRecordDao.remove(accountId, taskId);
      if (hasDeleted) {
        logger.info("Deleted the perpetual task with id={}.", taskId);
      }
      return hasDeleted;
    }
  }

  @Override
  public List<PerpetualTaskAssignDetails> listAssignedTasks(String delegateId) {
    String accountId = DelegateAuthServerInterceptor.ACCOUNT_ID_CTX_KEY.get(Context.current());

    List<PerpetualTaskRecord> taskRecords = perpetualTaskRecordDao.listAssignedTasks(delegateId, accountId);

    return taskRecords.stream()
        .map(task
            -> PerpetualTaskAssignDetails.newBuilder()
                   .setTaskId(PerpetualTaskId.newBuilder().setId(task.getUuid()))
                   .setLastContextUpdated(HTimestamps.fromMillis(task.getClientContext().getLastContextUpdated()))
                   .build())
        .collect(Collectors.toList());
  }

  @Override
  public PerpetualTaskRecord getTaskRecord(String taskId) {
    return perpetualTaskRecordDao.getTask(taskId);
  }

  @Override
  public String getPerpetualTaskType(String taskId) {
    PerpetualTaskRecord perpetualTaskRecord = getTaskRecord(taskId);
    return perpetualTaskRecord.getPerpetualTaskType();
  }

  @Override
  public PerpetualTaskExecutionContext perpetualTaskContext(String taskId) {
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskRecordDao.getTask(taskId);

    PerpetualTaskExecutionParams params = getTaskParams(perpetualTaskRecord);

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromSeconds(perpetualTaskRecord.getIntervalSeconds()))
                                         .setTimeout(Durations.fromMillis(perpetualTaskRecord.getTimeoutMillis()))
                                         .build();

    return PerpetualTaskExecutionContext.newBuilder()
        .setTaskParams(params)
        .setTaskSchedule(schedule)
        .setHeartbeatTimestamp(HTimestamps.fromMillis(perpetualTaskRecord.getLastHeartbeat()))
        .build();
  }

  private PerpetualTaskExecutionParams getTaskParams(PerpetualTaskRecord perpetualTaskRecord) {
    PerpetualTaskServiceClient client = clientRegistry.getClient(perpetualTaskRecord.getPerpetualTaskType());
    Message perpetualTaskParams = client.getTaskParams(perpetualTaskRecord.getClientContext());
    return PerpetualTaskExecutionParams.newBuilder().setCustomizedParams(Any.pack(perpetualTaskParams)).build();
  }

  @Override
  public boolean triggerCallback(String taskId, long heartbeatMillis, PerpetualTaskResponse perpetualTaskResponse) {
    PerpetualTaskRecord taskRecord = perpetualTaskRecordDao.getTask(taskId);
    if (null == taskRecord || taskRecord.getLastHeartbeat() > heartbeatMillis) {
      return false;
    }
    boolean heartbeatUpdated = perpetualTaskRecordDao.saveHeartbeat(taskRecord, heartbeatMillis);
    perpetualTaskRecordDao.setTaskState(taskId, perpetualTaskResponse.getPerpetualTaskState().name());
    stateChangeCallback(taskId, perpetualTaskResponse);
    return heartbeatUpdated;
  }

  @Override
  public void setTaskState(String taskId, String state) {
    perpetualTaskRecordDao.setTaskState(taskId, state);
  }

  private void stateChangeCallback(String taskId, PerpetualTaskResponse perpetualTaskResponse) {
    PerpetualTaskRecord task = perpetualTaskRecordDao.getTask(taskId);
    PerpetualTaskServiceClient client = clientRegistry.getClient(task.getPerpetualTaskType());
    client.onTaskStateChange(task.getUuid(), perpetualTaskResponse, perpetualTaskResponse);
  }
}
