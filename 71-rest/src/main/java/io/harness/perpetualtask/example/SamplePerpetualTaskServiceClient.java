package io.harness.perpetualtask.example;

import com.google.inject.Inject;
import com.google.protobuf.Any;

import io.harness.perpetualtask.PerpetualTaskParams;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskServiceFactory;
import io.harness.perpetualtask.example.SampleTask.SamplePerpetualTaskParams;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SamplePerpetualTaskServiceClient implements PerpetualTaskServiceClient {
  SamplePerpetualTaskParams defaultParams;
  Map<String, SamplePerpetualTaskParams> taskParamsMap; // <taskId, params>
  PerpetualTaskService service;

  @Inject
  public SamplePerpetualTaskServiceClient(PerpetualTaskServiceFactory serviceFactory) {
    this.service = serviceFactory.getInstance();
    taskParamsMap = new ConcurrentHashMap<>();
    defaultParams = SamplePerpetualTaskParams.newBuilder().setCountry("defaultCountry").build();
  }

  public String createTask(SamplePerpetualTaskParams params) {
    String clientHandle = UUID.randomUUID().toString();

    PerpetualTaskSchedule config = PerpetualTaskSchedule.newBuilder().build();
    String recordId = service.createTask(this.getClass().getSimpleName(), clientHandle, config);
    taskParamsMap.put(clientHandle, params);
    return recordId;
  }

  public void deleteTask(String taskId) {
    service.deleteTask(this.getClass().getSimpleName(), taskId);
    taskParamsMap.remove(taskId);
  }

  @Override
  public PerpetualTaskParams getTaskParams(String clientHandle) {
    SamplePerpetualTaskParams params = taskParamsMap.get(clientHandle);
    if (clientHandle.endsWith("i")) {
      params = SamplePerpetualTaskParams.newBuilder().setCountry("India").build();
    }

    if (clientHandle.endsWith("u")) {
      params = SamplePerpetualTaskParams.newBuilder().setCountry("USA").build();
    }

    if (params == null) {
      params = defaultParams;
    }

    return PerpetualTaskParams.newBuilder().setCustomizedParams(Any.pack(params)).build();
  }
}