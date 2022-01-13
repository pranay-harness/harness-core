/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.migration.list;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;

import static java.time.Duration.ofMillis;

import io.harness.cvng.activity.entities.ActivitySource.ActivitySourceKeys;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.beans.activity.ActivitySourceType;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.ChecklistItem;
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
public class UpdateActivitySourceTasksMigration implements CVNGMigration {
  @Inject private HPersistence hPersistence;
  @Inject private VerificationManagerService verificationManagerService;
  @Inject private MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService;

  @Override
  public void migrate() {
    log.info("migration started");
    List<KubernetesActivitySource> kubernetesActivitySources =
        hPersistence.createQuery(KubernetesActivitySource.class, excludeAuthority)
            .filter(ActivitySourceKeys.type, ActivitySourceType.KUBERNETES)
            .asList();
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
        -> monitoringSourcePerpetualTaskService.createTask(cvConfigKey.getAccountId(), cvConfigKey.getOrgIdentifier(),
            cvConfigKey.getProjectIdentifier(), cvConfigKey.getConnectorIdentifier(),
            cvConfigKey.getMonitoringSourceIdentifier(), false));
    log.info("migration done");
  }

  @Override
  public ChecklistItem whatHappensOnRollback() {
    return ChecklistItem.NA;
  }

  @Override
  public ChecklistItem whatHappensIfOldVersionIteratorPicksMigratedEntity() {
    return ChecklistItem.NA;
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
