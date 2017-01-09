package software.wings.service.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.DockerConfig;
import software.wings.beans.DockerConfig.Builder;
import software.wings.helpers.ext.docker.DockerRegistryService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.DockerBuildService;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 1/6/17.
 */
@Ignore
public class DockerBuildServiceImplTest extends WingsBaseTest {
  @Inject private DockerBuildService dockerBuildService;
  @Inject private DockerRegistryService dockerRegistryService;

  @Test
  public void shouldGetBuilds() {
    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                                    .withRegistryUrl("https://registry.hub.docker.com/v2/")
                                    .withRegistryUsername("anubhaw")
                                    .withRegistryPassword("anubhaw@Dhub")
                                    .withDockerConfig(null)
                                    .build();
    DockerClient docker = DockerClientBuilder.getInstance(config).build();
    List<Image> imageList = docker.listImagesCmd().exec();
    imageList.forEach(image -> {
      for (String str : image.getRepoTags()) {
        System.out.println(str);
      }
    });
  }

  @Test
  public void shouldGetLastSuccessfulBuild() {
    DockerConfig dockerConfig = Builder.aDockerConfig()
                                    .withDockerRegistryUrl("https://registry.hub.docker.com/v2/")
                                    .withUsername("anubhaw")
                                    .withPassword("anubhaw@Dhub")
                                    .build();
    List<BuildDetails> builds = dockerRegistryService.getBuilds(dockerConfig, "anubhaw/my-docker-whale", 5);
    System.out.println(builds);
  }
}
