package io.harness.cdng.artifact.service;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSource;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.utils.WingsTestConstants;

public class ArtifactSourceDaoTest extends WingsBaseTest {
  @Inject @InjectMocks ArtifactSourceDao artifactSourceDao;

  DockerArtifactSource dockerArtifactSource = DockerArtifactSource.builder()
                                                  .dockerHubConnector("DOCKER_CONNECTOR")
                                                  .imagePath("imagePath")
                                                  .accountId(WingsTestConstants.ACCOUNT_ID)
                                                  .uniqueHash("DOCKER_UNIQUE_HASH")
                                                  .build();

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testCreateArtifactSourceForDocker() {
    ArtifactSource artifactSource = artifactSourceDao.create(dockerArtifactSource);
    assertThat(artifactSource).isInstanceOf(DockerArtifactSource.class);
    DockerArtifactSource artifactSourceResult = (DockerArtifactSource) artifactSource;
    assertThat(artifactSourceResult.getImagePath()).isEqualTo(dockerArtifactSource.getImagePath());
    assertThat(artifactSourceResult.getAccountId()).isEqualTo(dockerArtifactSource.getAccountId());
    assertThat(artifactSourceResult.getUniqueHash()).isEqualTo(dockerArtifactSource.getUniqueHash());

    ArtifactSource nextSourceResult = artifactSourceDao.get(WingsTestConstants.ACCOUNT_ID, artifactSource.getUuid());
    assertThat(nextSourceResult).isInstanceOf(DockerArtifactSource.class);
    DockerArtifactSource nextDockerSourceResult = (DockerArtifactSource) nextSourceResult;
    assertThat(nextDockerSourceResult.getUniqueHash()).isEqualTo(dockerArtifactSource.getUniqueHash());
    assertThat(nextDockerSourceResult.getImagePath()).isEqualTo(dockerArtifactSource.getImagePath());

    nextSourceResult =
        artifactSourceDao.getArtifactStreamByHash(WingsTestConstants.ACCOUNT_ID, artifactSource.getUniqueHash());
    assertThat(nextSourceResult).isInstanceOf(DockerArtifactSource.class);
    nextDockerSourceResult = (DockerArtifactSource) nextSourceResult;
    assertThat(nextDockerSourceResult.getUniqueHash()).isEqualTo(dockerArtifactSource.getUniqueHash());
    assertThat(nextDockerSourceResult.getImagePath()).isEqualTo(dockerArtifactSource.getImagePath());
  }
}