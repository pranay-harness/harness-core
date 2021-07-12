package io.harness.migrations.all;

import static io.harness.rule.OwnerRule.MOUNIK;

import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Service;
import software.wings.beans.artifact.*;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DeleteInvalidArtifactStreamsTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DeleteInvalidArtifactStreams deleteInvalidArtifactStreamMigration;

  @Test
  @Owner(developers = MOUNIK)
  @Category(UnitTests.class)
  public void shouldNotDeleteAnyArtifactStreams() {
    List<String> artifactStreamIds = new ArrayList<>(Arrays.asList("id1", "id2", "id3"));
    Service service = Service.builder().uuid(SERVICE_ID).artifactStreamIds(artifactStreamIds).build();
    wingsPersistence.save(service);
    List<ArtifactStream> artifactStreams = new LinkedList<>();
    artifactStreams.add(EcrArtifactStream.builder().uuid("id4").serviceId(SERVICE_ID).build());
    artifactStreams.add(AmazonS3ArtifactStream.builder().uuid("id2").serviceId(SERVICE_ID).build());
    artifactStreams.add(DockerArtifactStream.builder().uuid("id3").serviceId(SERVICE_ID).build());
    artifactStreams.add(EcrArtifactStream.builder().uuid("id1").serviceId(SERVICE_ID).build());
    wingsPersistence.save(artifactStreams);
    deleteInvalidArtifactStreamMigration.migrate();
    Service service1 = wingsPersistence.get(Service.class, service.getUuid());
    assertThat(service1.getArtifactStreamIds().equals(artifactStreamIds));
  }

  @Test
  @Owner(developers = MOUNIK)
  @Category(UnitTests.class)
  public void shouldDeleteArtifactStreams() {
    List<String> artifactStreamIds = new ArrayList<>(Arrays.asList("id1", "id2", "id3"));
    Service service = Service.builder().uuid(SERVICE_ID).artifactStreamIds(artifactStreamIds).build();
    wingsPersistence.save(service);
    List<ArtifactStream> artifactStreams = new LinkedList<>();
    artifactStreams.add(EcrArtifactStream.builder().uuid("id4").serviceId(SERVICE_ID).build());
    artifactStreams.add(SmbArtifactStream.builder().uuid("id2").serviceId(SERVICE_ID).build());
    wingsPersistence.save(artifactStreams);
    deleteInvalidArtifactStreamMigration.migrate();
    Service service1 = wingsPersistence.get(Service.class, service.getUuid());
    List<String> requiredArtifacts = new ArrayList<>(Arrays.asList("id2"));
    assertThat(service1.getArtifactStreamIds().equals(requiredArtifacts));
  }
}
