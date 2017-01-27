package software.wings.service.impl;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.DockerConfig;
import software.wings.beans.DockerConfig.Builder;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.helpers.ext.docker.DockerRegistryService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.rules.Integration;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.utils.ArtifactType;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 1/6/17.
 */
@Ignore
@Integration
public class DockerBuildServiceImplTest extends WingsBaseTest {
  @Inject private DockerRegistryService dockerRegistryService;
  @Inject private ArtifactStreamService artifactStreamService;

  @Test
  public void shouldGetBuilds() {
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.Builder.aDockerArtifactStream()
                                                    .withAppId("UXGI1f4vQa6nt5eXBcnv7A")
                                                    .withImageName("library/mysql")
                                                    .withAutoDownload(true)
                                                    .withAutoApproveForProduction(true)
                                                    .withSettingId("knCLyrVjRjyUYM15RcjUQQ")
                                                    .withSourceName(ArtifactType.DOCKER.name())
                                                    .withServiceId("jf_6aiHZRFGpKTJF7yDHgg")
                                                    .build();
    ArtifactStream artifactStream = artifactStreamService.create(dockerArtifactStream);
    System.out.println(artifactStream.toString());
  }

  @Test
  public void shouldGetLastSuccessfulBuild() {
    DockerConfig dockerConfig = Builder.aDockerConfig()
                                    .withDockerRegistryUrl("https://registry.hub.docker.com/v2/")
                                    .withUsername("anubhaw")
                                    .withPassword("anubhaw@Dhub")
                                    .build();
    List<BuildDetails> builds = dockerRegistryService.getBuilds(dockerConfig, "library/mysql", 5);
    System.out.println(builds);
  }
}
