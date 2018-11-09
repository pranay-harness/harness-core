package software.wings.service.impl.artifact;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.EmbeddedUser;
import org.junit.Test;
import software.wings.beans.artifact.Artifact;
import software.wings.common.Constants;

import java.util.List;

public class ArtifactComparatorTest {
  private Artifact.Builder artifactBuilder = anArtifact()
                                                 .withAppId(APP_ID)
                                                 .withArtifactStreamId(ARTIFACT_STREAM_ID)
                                                 .withRevision("1.0")
                                                 .withDisplayName("DISPLAY_NAME")
                                                 .withCreatedAt(System.currentTimeMillis())
                                                 .withCreatedBy(EmbeddedUser.builder().uuid("USER_ID").build())
                                                 .withServiceIds(asList(SERVICE_ID));

  @Test
  public void shouldSortArtifactDescendingOrder() {
    List<Artifact> artifacts = asList(
        artifactBuilder.withMetadata(ImmutableMap.of(Constants.BUILD_NO, "todolist-1.0-1.x86_64.rpm")).but().build(),
        artifactBuilder.withMetadata(ImmutableMap.of(Constants.BUILD_NO, "todolist-1.0-10.x86_64.rpm")).but().build(),
        artifactBuilder.withMetadata(ImmutableMap.of(Constants.BUILD_NO, "todolist-1.0-5.x86_64.rpm")).but().build(),
        artifactBuilder.withMetadata(ImmutableMap.of(Constants.BUILD_NO, "todolist-1.0-15.x86_64.rpm")).but().build());
    assertThat(artifacts.stream().sorted(new ArtifactComparator()).collect(toList()))
        .hasSize(4)
        .extracting(Artifact::getBuildNo)
        .containsSequence("todolist-1.0-15.x86_64.rpm", "todolist-1.0-10.x86_64.rpm", "todolist-1.0-5.x86_64.rpm",
            "todolist-1.0-1.x86_64.rpm");
  }
}
