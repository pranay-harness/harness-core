package io.harness;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import graphql.VisibleForTesting;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.dl.WingsPersistence;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

@Slf4j
@Singleton
@OwnedBy(CDC)
public class DeleteInvalidArtifactStreams {
  private static final String DEBUG_LINE = "[DELETE_INVALID_ARTIFACT_STREAMS_MIGRATION]: ";
  @Inject private WingsPersistence wingsPersistence;

  public void migrate() {
    log.info(String.join(DEBUG_LINE, "Starting Migration"));
    try (HIterator<Account> accounts = new HIterator<>(wingsPersistence.createQuery(Account.class).fetch())) {
      while (accounts.hasNext()) {
        Account account = accounts.next();
        log.info(String.join(DEBUG_LINE, " Starting Migration For account ", account.getAccountName()));
        migrateInvalidArtifactStreams(account);
      }
    } catch (Exception ex) {
      log.error(String.join(DEBUG_LINE, " Exception while fetching Accounts"));
    }
  }

  private void migrateInvalidArtifactStreams(Account account) {
    Set<String> artifactStreamIdSet = new HashSet<>();
    try (HIterator<ArtifactStream> artifactStreams =
             new HIterator<>(wingsPersistence.createQuery(ArtifactStream.class)
                                 .filter(ArtifactStreamKeys.accountId, account.getUuid())
                                 .fetch())) {
      log.info(String.join(DEBUG_LINE, " Fetching artifact streams for account ", account.getAccountName(), "with Id",
          account.getUuid()));
      while (artifactStreams.hasNext()) {
        ArtifactStream artifactStream = artifactStreams.next();
        artifactStreamIdSet.add(artifactStream.getUuid());
      }
    } catch (Exception ex) {
      log.error(
          String.join(DEBUG_LINE, " Exception while fetching artifact streams with account Id ", account.getUuid()));
    }
    Set<Service> serviceSet = new HashSet<>();
    try (HIterator<Service> services = new HIterator<>(
             wingsPersistence.createQuery(Service.class).filter(ServiceKeys.accountId, account.getUuid()).fetch())) {
      log.info(String.join(
          DEBUG_LINE, " Fetching services for account ", account.getAccountName(), "with Id", account.getUuid()));
      while (services.hasNext()) {
        Service service = services.next();
        if (service != null) {
          serviceSet.add(service);
        }
      }
    } catch (Exception ex) {
      log.error(String.join(DEBUG_LINE, " Exception while fetching services with account Id ", account.getUuid()));
    }
    migrate(artifactStreamIdSet, serviceSet);
    artifactStreamIdSet.clear();
    serviceSet.clear();
  }

  @VisibleForTesting
  void migrate(Set<String> artifactStreamIdSet, Set<Service> serviceSet) {
    try {
      if (isNotEmpty(artifactStreamIdSet)) {
        for (Service service : serviceSet) {
          System.out.println(service.getUuid());
          List<String> artifactStreamIds = service.getArtifactStreamIds();
          System.out.println(artifactStreamIds);
          if (isNotEmpty(artifactStreamIds)) {
            artifactStreamIds.removeIf(id -> !artifactStreamIdSet.contains(id));
            wingsPersistence.updateField(
                Service.class, service.getUuid(), ServiceKeys.artifactStreamIds, artifactStreamIds);
          }
        }
      }
    } catch (RuntimeException e) {
      log.error(String.join(DEBUG_LINE, "Failed With RuntimeException ", e.getMessage()));
    } catch (Exception e) {
      log.error(String.join(DEBUG_LINE, "Failed With Exception ", e.getMessage()));
    }
  }
}
