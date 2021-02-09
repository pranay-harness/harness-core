package io.harness.cvng.migration.list;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;

import static java.time.Duration.ofMillis;

import io.harness.cvng.activity.entities.ActivitySource.ActivitySourceKeys;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.services.api.MonitoringTaskPerpetualTaskService;
import io.harness.cvng.migration.CNVGMigration;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateActivitySourceTasksMigration implements CNVGMigration {
  @Inject private HPersistence hPersistence;
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private MonitoringTaskPerpetualTaskService monitoringTaskPerpetualTaskService;

  @Override
  public void migrate() {
    log.info("migration started");
    List<KubernetesActivitySource> kubernetesActivitySources =
        hPersistence.createQuery(KubernetesActivitySource.class, excludeAuthority).asList();
    Set<CVConfigKey> cvConfigKeys = new HashSet<>();
    kubernetesActivitySources.stream()
        .filter(kubernetesActivitySource -> isNotEmpty(kubernetesActivitySource.getDataCollectionTaskId()))
        .forEach(kubernetesActivitySource -> {
          try {
            log.info("deleting perpetual task for {}", kubernetesActivitySource);
            // set iterator to not execute for next 5 mins
            hPersistence.update(kubernetesActivitySource,
                hPersistence.createUpdateOperations(KubernetesActivitySource.class)
                    .set(ActivitySourceKeys.dataCollectionTaskIteration,
                        Instant.now().plus(5, ChronoUnit.MINUTES).toEpochMilli()));
            verificationManagerService.deletePerpetualTask(
                kubernetesActivitySource.getAccountId(), kubernetesActivitySource.getDataCollectionTaskId());
            hPersistence.update(kubernetesActivitySource,
                hPersistence.createUpdateOperations(KubernetesActivitySource.class)
                    .unset(ActivitySourceKeys.dataCollectionTaskId));
            sleep(ofMillis(100));
          } catch (Exception e) {
            log.error("error deleting perpetual task for {}", kubernetesActivitySource, e);
          }
        });

    cvConfigKeys.forEach(cvConfigKey
        -> monitoringTaskPerpetualTaskService.createTask(cvConfigKey.getAccountId(), cvConfigKey.getOrgIdentifier(),
            cvConfigKey.getProjectIdentifier(), cvConfigKey.getConnectorIdentifier(),
            cvConfigKey.getMonitoringSourceIdentifier()));
    log.info("migration done");
  }

  @Value
  @Builder
  private static class CVConfigKey {
    private String accountId;
    private String orgIdentifier;
    private String projectIdentifier;
    private String connectorIdentifier;
    private String monitoringSourceIdentifier;
  }
}
