package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import graphql.VisibleForTesting;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CDC)
public class DeleteInvalidArtifactStreams implements Migration {
  private static final String DEBUG_LINE = "[DELETE_INVALID_ARTIFACT_STREAMS_MIGRATION]: ";
  @Inject private WingsPersistence wingsPersistence;
  @Inject ServiceResourceService serviceResourceService;
  @Override
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
    List<ArtifactStream> artifactStreamList = new LinkedList<>();
    try (HIterator<ArtifactStream> artifactStreams =
             new HIterator<>(wingsPersistence.createQuery(ArtifactStream.class)
                                 .filter(ArtifactStream.ArtifactStreamKeys.accountId, account.getUuid())
                                 .fetch())) {
      log.info(String.join(DEBUG_LINE, " Fetching artifact streams for account ", account.getAccountName(), "with Id",
          account.getUuid()));
      while (artifactStreams.hasNext()) {
        ArtifactStream artifactStream = artifactStreams.next();
        artifactStreamIdSet.add(artifactStream.getArtifactStreamId());
        artifactStreamList.add(artifactStream);
      }
    } catch (Exception ex) {
      log.error(
          String.join(DEBUG_LINE, " Exception while fetching artifact streams with account Id ", account.getUuid()));
    }
    try {
      Set<String> serviceIds = new HashSet<>();
      for (ArtifactStream artifactStream : artifactStreamList) {
        String serviceId = artifactStream.getServiceId();
        if (serviceIds.contains(serviceId)) {
          continue;
        }
        serviceIds.add(serviceId);
        Service service = serviceResourceService.get(artifactStream.getAppId(), serviceId);
        if (service == null) {
          log.info("Artifact Stream with id {} of non existent service id {} found", artifactStream.getUuid(),
              artifactStream.getServiceId());
        } else {
          List<String> artifactStreamIds = service.getArtifactStreamIds();
          for (String id : artifactStreamIds) {
            if (!(artifactStreamIdSet.contains(id))) {
              artifactStreamIds.remove(id);
            }
          }
          wingsPersistence.updateField(
              Service.class, serviceId, Service.ServiceKeys.artifactStreamIds, artifactStreamIds);
        }
      }
      artifactStreamIdSet.clear();
      serviceIds.clear();
      artifactStreamList.clear();
    } catch (RuntimeException e) {
      log.error(String.join(DEBUG_LINE, "Failed With RuntimeException ", e.getMessage()));
    } catch (Exception e) {
      log.error(String.join(DEBUG_LINE, "Failed With Exception ", e.getMessage()));
    }
  }

  @VisibleForTesting
  void migrate(Set<String> artifactStreamIdSet) {
    try {
    } catch (RuntimeException e) {
      log.error(String.join(DEBUG_LINE, "Failed With RuntimeException ", e.getMessage()));
    } catch (Exception e) {
      log.error(String.join(DEBUG_LINE, "Failed With Exception ", e.getMessage()));
    }
  }
}
