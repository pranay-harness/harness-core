package io.harness.delegate.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.network.SafeHttpCall.execute;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.rest.RestResponse;
import io.harness.verificationclient.CVNextGenServiceClient;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class KubernetesActivitiesStoreService {
  @Inject private CVNextGenServiceClient cvNextGenServiceClient;
  @Inject private TimeLimiter timeLimiter;
  private Cache<String, List<KubernetesActivityDTO>> activitiesCache;

  @Inject
  public KubernetesActivitiesStoreService(CVNextGenServiceClient cvNextGenServiceClient,
      @Named("asyncExecutor") ExecutorService executorService, TimeLimiter timeLimiter) {
    this.cvNextGenServiceClient = cvNextGenServiceClient;
    this.timeLimiter = timeLimiter;
    this.activitiesCache = Caffeine.newBuilder()
                               .executor(executorService)
                               .expireAfterWrite(1000, TimeUnit.MILLISECONDS)
                               .removalListener(this::dispatchKubernetesActivities)
                               .build();
  }

  public synchronized void save(String accountId, KubernetesActivityDTO activityDTO) {
    Optional.ofNullable(activitiesCache.get(accountId, s -> new ArrayList<>()))
        .ifPresent(activityDTOs -> activityDTOs.add(activityDTO));
  }

  private void dispatchKubernetesActivities(
      String accountId, List<KubernetesActivityDTO> activityDTOS, RemovalCause removalCause) {
    if (accountId == null || isEmpty(activityDTOS)) {
      log.error("Unexpected Cache eviction accountId={}, activityDTOS={}, removalCause={}", accountId, activityDTOS,
          removalCause);
      return;
    }
    activityDTOS.stream()
        .collect(groupingBy(KubernetesActivityDTO::getActivitySourceConfigId, toList()))
        .forEach((activitySourceConfigId, activities) -> {
          if (isEmpty(activities)) {
            return;
          }
          try {
            log.info("Dispatching {} activities for [{}] [{}]", activities.size(), accountId, activitySourceConfigId);
            RestResponse<Boolean> restResponse = timeLimiter.callWithTimeout(
                ()
                    -> execute(
                        cvNextGenServiceClient.saveKubernetesActivities(accountId, activitySourceConfigId, activities)),
                30, TimeUnit.SECONDS, true);
            if (restResponse == null) {
              return;
            }
            log.info("Dispatched {} activities for [{}] [{}]",
                restResponse.getResource() != null ? activities.size() : 0, accountId, activitySourceConfigId);
          } catch (Exception e) {
            log.error(
                "Dispatch activities failed for {}. printing lost activities[{}]", accountId, activities.size(), e);
            activities.forEach(logObject -> log.error(logObject.toString()));
            log.error("Finished printing lost activities");
          }
        });
  }
}
