/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Account;
import software.wings.beans.Service;
import software.wings.beans.Service.ServiceKeys;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import graphql.VisibleForTesting;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CDC)
public class DeleteInvalidArtifactStreams implements Migration {
  private static final String DEBUG_LINE = "[DELETE_INVALID_ARTIFACT_STREAMS_MIGRATION]: ";
  @Inject private WingsPersistence wingsPersistence;
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
    try (HIterator<Service> services = new HIterator<>(
             wingsPersistence.createQuery(Service.class).filter(ServiceKeys.accountId, account.getUuid()).fetch())) {
      log.info(String.join(
          DEBUG_LINE, " Fetching services for account ", account.getAccountName(), "with Id", account.getUuid()));
      while (services.hasNext()) {
        Service service = services.next();
        if (service != null) {
          migrate(artifactStreamIdSet, service);
        }
      }
    } catch (Exception ex) {
      log.error(String.join(DEBUG_LINE, " Exception while fetching services with account Id ", account.getUuid()));
    }
    artifactStreamIdSet.clear();
  }

  @VisibleForTesting
  void migrate(Set<String> artifactStreamIdSet, Service service) {
    try {
      List<String> artifactStreamIds = service.getArtifactStreamIds();
      if (isNotEmpty(artifactStreamIds)) {
        int size = artifactStreamIds.size();
        artifactStreamIds.removeIf(id -> !artifactStreamIdSet.contains(id));
        if (size != artifactStreamIds.size()) {
          wingsPersistence.updateField(
              Service.class, service.getUuid(), ServiceKeys.artifactStreamIds, artifactStreamIds);
          log.info(String.join(DEBUG_LINE, " Invalid Artifact Deletion Successful for service ", service.getName(),
              "with Id ", service.getUuid()));
        }
      }
    } catch (RuntimeException e) {
      log.error(String.join(DEBUG_LINE, "Migration Failed With RuntimeException ", e.getMessage(),
          "for service with Id ", service.getUuid()));
    } catch (Exception e) {
      log.error(String.join(
          DEBUG_LINE, "Migration Failed With Exception ", e.getMessage(), "for service with Id ", service.getUuid()));
    }
  }
}
