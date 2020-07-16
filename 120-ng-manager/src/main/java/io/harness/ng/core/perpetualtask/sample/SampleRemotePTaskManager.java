package io.harness.ng.core.perpetualtask.sample;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.util.Durations;

import io.harness.ManagerDelegateServiceDriver;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.perpetualtask.RemotePerpetualTaskClientContext;
import io.harness.perpetualtask.RemotePerpetualTaskSchedule;
import io.harness.perpetualtask.remote.RemotePerpetualTaskType;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

@Singleton
@Slf4j
public class SampleRemotePTaskManager {
  static final String COUNTRY_NAME = "countryName";
  private ManagerDelegateServiceDriver managerDelegateServiceDriver;
  private final ConcurrentMap<String, Integer> countryMap = new ConcurrentHashMap<>();

  @Inject
  public SampleRemotePTaskManager(ManagerDelegateServiceDriver managerDelegateServiceDriver) {
    this.managerDelegateServiceDriver = managerDelegateServiceDriver;
  }

  public String create(String accountId, String countryName, int population) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Creating a Sample Remote Perpetual Task.");
      countryMap.putIfAbsent(countryName, population);
      Map<String, String> clientParamMap = new HashMap<>();
      clientParamMap.put(COUNTRY_NAME, countryName);
      RemotePerpetualTaskSchedule schedule = RemotePerpetualTaskSchedule.newBuilder()
                                                 .setInterval(Durations.fromMinutes(1))
                                                 .setTimeout(Durations.fromSeconds(30))
                                                 .build();
      final RemotePerpetualTaskClientContext context =
          RemotePerpetualTaskClientContext.newBuilder().putAllTaskClientParams(clientParamMap).build();

      return managerDelegateServiceDriver.createRemotePerpetualTask(
          RemotePerpetualTaskType.REMOTE_SAMPLE.getTaskType(), accountId, context, schedule, false);
    }
  }

  public boolean update(String accountId, String taskId, String countryName, int population) {
    countryMap.put(countryName, population);
    return managerDelegateServiceDriver.resetRemotePerpetualTask(accountId, taskId);
  }

  public boolean delete(String accountId, String taskId) {
    return managerDelegateServiceDriver.deleteRemotePerpetualTask(accountId, taskId);
  }

  public int getPopulation(String countryName) {
    return countryMap.getOrDefault(countryName, ThreadLocalRandom.current().nextInt(10, 1000));
  }
}
