package io.harness;

import com.google.inject.Inject;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Service;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.SmbArtifactStream;
import software.wings.dl.WingsPersistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static io.harness.rule.OwnerRule.MOUNIK;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

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
    artifactStreams.add(SmbArtifactStream.builder().uuid("id2").serviceId(SERVICE_ID).build());
    wingsPersistence.save(artifactStreams);
    deleteInvalidArtifactStreamMigration.migrate();
    Service service1 = wingsPersistence.get(Service.class, service.getUuid());
    System.out.println(service1.getArtifactStreamIds());
    List<String> requiredArtifacts = new ArrayList<>(Arrays.asList("id2"));
    assertThat(service1.getArtifactStreamIds().equals(requiredArtifacts)).isTrue();
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
    assertThat(service1.getArtifactStreamIds().equals(requiredArtifacts)).isTrue();
  }
}
