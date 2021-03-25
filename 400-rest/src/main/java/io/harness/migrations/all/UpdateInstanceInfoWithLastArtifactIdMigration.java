package io.harness.migrations.all;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactKeys;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceKeys;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.K8sContainerInfo;
import software.wings.beans.infrastructure.instance.info.K8sPodInfo;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class UpdateInstanceInfoWithLastArtifactIdMigration implements Migration {
  String debugLog = "InstanceInfo_LastArtifactId_Migration: ";

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info(debugLog + "Starting UpdateInstanceInfoWithLastArtifactId migration");

    try (HIterator<Account> accounts =
             new HIterator<>(wingsPersistence.createQuery(Account.class, excludeAuthority).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        if (account == null) {
          log.info(debugLog + "Account is null,  continuing");
          continue;
        }

        log.info(debugLog + "Starting migration  for account {}", account.getAccountName());
        try (HIterator<Application> applications =
                 new HIterator<>(wingsPersistence.createQuery(Application.class)
                                     .filter(ApplicationKeys.accountId, account.getUuid())
                                     .fetch())) {
          while (applications.hasNext()) {
            Application application = applications.next();
            if (application == null) {
              log.info(debugLog + "Application is null, skipping");
              continue;
            }

            log.info(debugLog + "Starting migration for  application {}", application.getName());
            try (HIterator<Instance> instances = new HIterator<>(wingsPersistence.createQuery(Instance.class)
                                                                     .filter(InstanceKeys.appId, application.getUuid())
                                                                     .filter(InstanceKeys.isDeleted, false)
                                                                     .fetch())) {
              while (instances.hasNext()) {
                Instance instance = instances.next();
                InstanceInfo instanceInfo = instance.getInstanceInfo();

                if (instanceInfo instanceof K8sPodInfo) {
                  List<K8sContainerInfo> containers = ((K8sPodInfo) instanceInfo).getContainers();

                  if (hasNone(containers)) {
                    continue;
                  }

                  for (K8sContainerInfo k8sContainerInfo : containers) {
                    Artifact artifact = wingsPersistence.createQuery(Artifact.class)
                                            .filter(ArtifactKeys.artifactStreamId, instance.getLastArtifactStreamId())
                                            .filter(ArtifactKeys.appId, instance.getAppId())
                                            .filter("metadata.image", k8sContainerInfo.getImage())
                                            .disableValidation()
                                            .get();
                    if (artifact != null) {
                      String artifactUuid = artifact.getUuid();

                      if (!artifactUuid.equals(instance.getLastArtifactId())) {
                        instance.setLastArtifactId(artifactUuid);
                        wingsPersistence.save(instance);
                        log.info(debugLog + "Updated instance {}", instance.getUuid());
                        break;
                      }
                    }
                  }
                }
              }
            }
          }
        }
        log.info(debugLog + "Migration done for account {}", account.getAccountName());
      }
    }
    log.info(debugLog + "Completed UpdateInstanceInfoWithLastArtifactId migration");
  }
}
