package io.harness;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import graphql.VisibleForTesting;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HIterator;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Service;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.SmbArtifactStream;
import software.wings.dl.WingsPersistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.rule.OwnerRule.MOUNIK;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CDC)
public class DeleteInvalidArtifactStreamsTest extends WingsBaseTest {
  private static final String DEBUG_LINE = "[DELETE_INVALID_ARTIFACT_STREAMS_MIGRATION]: ";
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DeleteInvalidArtifactStreams deleteInvalidArtifactStreamMigration;


  @Test
  @Owner(developers = MOUNIK)
  @Category(UnitTests.class)
  public void shouldNotDeleteAnyArtifactStreams() {
    List<String> artifactStreamIds = new ArrayList<>(Arrays.asList("id1", "id2", "id3"));
    Account account = Account.Builder.anAccount().build();
    account.setUuid(ACCOUNT_ID);
    wingsPersistence.save(account);
    Service service =
            Service.builder().accountId(ACCOUNT_ID).uuid(SERVICE_ID).artifactStreamIds(artifactStreamIds).build();
    wingsPersistence.save(service);
    List<ArtifactStream> artifactStreams = new LinkedList<>();
    artifactStreams.add(EcrArtifactStream.builder().accountId(ACCOUNT_ID).uuid("id4").serviceId(SERVICE_ID).build());
    artifactStreams.add(
            AmazonS3ArtifactStream.builder().accountId(ACCOUNT_ID).uuid("id2").serviceId(SERVICE_ID).build());
    artifactStreams.add(DockerArtifactStream.builder().accountId(ACCOUNT_ID).uuid("id3").serviceId(SERVICE_ID).build());
    artifactStreams.add(SmbArtifactStream.builder().accountId(ACCOUNT_ID).uuid("id1").serviceId(SERVICE_ID).build());
    wingsPersistence.save(artifactStreams);
    deleteInvalidArtifactStreamMigration.migrate();
    Service service1 = wingsPersistence.get(Service.class, service.getUuid());
    assertThat(service1.getArtifactStreamIds().equals(artifactStreamIds)).isTrue();
  }

  @Test
  @Owner(developers = MOUNIK)
  @Category(UnitTests.class)
  public void shouldDeleteArtifactStreams() {
    List<String> artifactStreamIds = new ArrayList<>(Arrays.asList("id1", "id5", "id3"));
    Account account = Account.Builder.anAccount().build();
    account.setUuid(ACCOUNT_ID);
    wingsPersistence.save(account);
    Service service =
            Service.builder().accountId(ACCOUNT_ID).uuid(SERVICE_ID).artifactStreamIds(artifactStreamIds).build();
    wingsPersistence.save(service);
    List<ArtifactStream> artifactStreams = new LinkedList<>();
    artifactStreams.add(SmbArtifactStream.builder().accountId(ACCOUNT_ID).uuid("id4").serviceId(SERVICE_ID).build());
    artifactStreams.add(SmbArtifactStream.builder().accountId(ACCOUNT_ID).uuid("id2").serviceId(SERVICE_ID).build());
    wingsPersistence.save(artifactStreams);
    deleteInvalidArtifactStreamMigration.migrate();
    Service service1 = wingsPersistence.get(Service.class, service.getUuid());
    List<String> requiredArtifacts = null;
    assertThat(service1.getArtifactStreamIds() == null).isTrue();
  }


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
                         .filter(ArtifactStream.ArtifactStreamKeys.accountId, account.getUuid())
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
            wingsPersistence.createQuery(Service.class).filter(Service.ServiceKeys.accountId, account.getUuid()).fetch())) {
      log.info(String.join(
              DEBUG_LINE, " Fetching services for account ", account.getAccountName(), "with Id", account.getUuid()));
      while (services.hasNext()) {
        System.out.println(2405);
        Service service = services.next();
        if (service != null) {
          migrate(artifactStreamIdSet, service);
        }
      }
    } catch (Exception ex) {
      log.error(String.join(DEBUG_LINE, " Exception while fetching services with account Id ", account.getUuid()));
    }
    artifactStreamIdSet.clear();
    serviceSet.clear();
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
                  Service.class, service.getUuid(), Service.ServiceKeys.artifactStreamIds, artifactStreamIds);
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
