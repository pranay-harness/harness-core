package io.harness.perpetualtask.example;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.perpetualtask.example.SamplePerpetualTaskServiceClient.COUNTRY_NAME;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.util.Durations;

import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.intfc.AccountService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Singleton
public class SamplePTaskServiceImpl implements SamplePTaskService {
  @Inject private AccountService accountService;
  @Inject private PerpetualTaskService perpetualTaskService;

  private ConcurrentMap<String, Integer> countryMap = new ConcurrentHashMap<>();

  @Override
  public String create(String accountId, String countryName, int population) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Creating a Sample Perpetual Task.");
      countryMap.putIfAbsent(countryName, population);
      Map<String, String> clientParamMap = new HashMap<>();
      clientParamMap.put(COUNTRY_NAME, countryName);
      PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(clientParamMap);
      PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                           .setInterval(Durations.fromMinutes(1))
                                           .setTimeout(Durations.fromSeconds(30))
                                           .build();
      return perpetualTaskService.createTask(PerpetualTaskType.SAMPLE, accountId, clientContext, schedule, false);
    }
  }

  @Override
  public boolean update(String accountId, String taskId, String countryName, int population) {
    countryMap.put(countryName, population);
    perpetualTaskService.resetTask(accountId, taskId);
    return true;
  }

  @Override
  public int getPopulation(String countryName) {
    return countryMap.get(countryName);
  }
}
