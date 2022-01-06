/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.MOUNIK;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Service;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.SmbArtifactStream;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
public class DeleteInvalidArtifactStreamsTest extends WingsBaseTest {
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
    List<String> artifactStreamIds = new ArrayList<>(Arrays.asList("id1", "id2", "id3"));
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
    List<String> requiredArtifacts = new ArrayList<>(Arrays.asList("id2"));
    assertThat(service1.getArtifactStreamIds().equals(requiredArtifacts)).isTrue();
  }
}
